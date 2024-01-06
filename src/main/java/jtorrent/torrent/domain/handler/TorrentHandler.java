package jtorrent.torrent.domain.handler;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level;
import static java.lang.System.getLogger;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jtorrent.common.domain.Constants;
import jtorrent.common.domain.model.Block;
import jtorrent.common.domain.util.BackgroundTask;
import jtorrent.common.domain.util.PeriodicTask;
import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.peer.domain.communication.PeerSocket;
import jtorrent.peer.domain.handler.PeerHandler;
import jtorrent.peer.domain.model.Peer;
import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.torrent.domain.repository.PieceRepository;
import jtorrent.tracker.domain.handler.TrackerHandler;
import jtorrent.tracker.domain.handler.factory.TrackerHandlerFactory;
import jtorrent.tracker.domain.model.PeerResponse;

public class TorrentHandler implements TrackerHandler.Listener, PeerHandler.EventHandler {

    private static final Logger LOGGER = getLogger(TorrentHandler.class.getName());

    private final Torrent torrent;
    private final Set<TrackerHandler> trackerHandlers;
    private final Set<PeerHandler> peerHandlers = new HashSet<>();
    private final Map<Integer, Set<PeerHandler>> pieceIndexToAvailablePeerHandlers = new HashMap<>();
    private final WorkDispatcher workDispatcher = new WorkDispatcher();
    private final PieceRepository repository;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final UnchokeTask unchokeTask = new UnchokeTask(executorService);
    /**
     * Set of peer contacts that are currently being connected to.
     */
    private final Set<PeerContactInfo> pendingContacts = ConcurrentHashMap.newKeySet();
    private final List<Listener> listeners = new ArrayList<>();
    /**
     * Used to prevent concurrent modification to the state of pieces.
     */
    private final Object pieceStateLock = new Object();
    /**
     * Used to prevent verification from happening while handling a successful peer connection.
     */
    private final Object verificationLock = new Object();

    public TorrentHandler(Torrent torrent, PieceRepository pieceRepository) {
        this.torrent = requireNonNull(torrent);
        this.repository = requireNonNull(pieceRepository);

        trackerHandlers = torrent.getTrackers().stream()
                .map(tracker -> TrackerHandlerFactory.create(torrent, tracker))
                .collect(Collectors.toSet());
        trackerHandlers.forEach(trackerHandler -> trackerHandler.addListener(this));
    }

    public void start() {
        torrent.setIsActive(true);
        CompletableFuture.runAsync(this::verifyFiles)
                .thenAccept(ignored -> {
                    workDispatcher.start();
                    trackerHandlers.forEach(TrackerHandler::start);
                    unchokeTask.scheduleAtFixedRate(0, 10, SECONDS);
                }).exceptionally(throwable -> {
                    log(Level.ERROR, "Failed to start", throwable);
                    return null;
                });
    }

    public void stop() {
        torrent.setIsActive(false);
        workDispatcher.stop();
        trackerHandlers.forEach(TrackerHandler::stop);
        unchokeTask.stop();
        executorService.shutdownNow();
        peerHandlers.forEach(PeerHandler::stop);
        torrent.clearPeers();
    }

    private void verifyFiles() {
        synchronized (pieceStateLock) {
            IntStream.range(0, torrent.getNumPieces())
                    .forEach(piece -> {
                        if (isPieceChecksumValid(piece)) {
                            torrent.setPieceVerified(piece);
                        } else {
                            torrent.setPieceMissing(piece);
                        }
                    });
        }
    }

    public void handleInboundPeerConnection(PeerSocket peerSocket) {
        PeerContactInfo peerContactInfo = peerSocket.getPeerContactInfo();

        if (isAlreadyConnectedOrPending(peerContactInfo)) {
            LOGGER.log(Level.DEBUG, "[{0}] Already connected or pending connection {0}", peerContactInfo);
            return;
        }

        Peer peer = new Peer(peerSocket.getPeerContactInfo());
        PeerHandler peerHandler = new PeerHandler(peer, peerSocket, this);
        connectPeerHandler(peerHandler);
    }

    public void handleDiscoveredPeerContact(PeerContactInfo peerContactInfo) {
        if (isAlreadyConnectedOrPending(peerContactInfo)) {
            LOGGER.log(Level.DEBUG, "[{0}] Already connected or pending connection {0}", peerContactInfo);
            return;
        }

        PeerSocket peerSocket = new PeerSocket();
        Peer peer = new Peer(peerContactInfo);
        PeerHandler peerHandler = new PeerHandler(peer, peerSocket, this);
        connectPeerHandler(peerHandler);
    }

    private void connectPeerHandler(PeerHandler peerHandler) {
        peerHandler.connect(torrent.getInfoHash(), true)
                .whenComplete((isDhtSupportedByRemote, throwable) -> {
                    if (throwable != null) {
                        log(Level.ERROR, String.format("[%s] Failed to connect", peerHandler.getPeerContactInfo()),
                                throwable);
                    } else {
                        handleConnectionSuccess(peerHandler, isDhtSupportedByRemote);
                    }
                    pendingContacts.remove(peerHandler.getPeerContactInfo());
                });
    }

    private void handleConnectionSuccess(PeerHandler peerHandler, boolean isDhtSupportedByRemote) {
        log(Level.INFO, String.format("[%s] Connected", peerHandler.getPeerContactInfo()));
        try {
            synchronized (verificationLock) {
                BitSet verifiedPieces = torrent.getVerifiedPieces();
                if (!verifiedPieces.isEmpty()) {
                    peerHandler.sendBitfield(verifiedPieces, torrent.getNumPieces());
                }
                if (isDhtSupportedByRemote) {
                    peerHandler.sendPort(Constants.PORT);
                }
                if (!torrent.isAllPiecesVerified()) {
                    peerHandler.sendInterested();
                }
                peerHandlers.add(peerHandler);
            }
            torrent.addPeer(peerHandler.getPeer());
            workDispatcher.addPeerHandler(peerHandler);
            peerHandler.start();
        } catch (IOException e) {
            log(Level.ERROR, String.format("Error handling connection success: %s", peerHandler.getPeerContactInfo()),
                    e);
        }
    }

    /**
     * Checks if the remote peer is already connected to or if a connection is pending.
     *
     * @param peerContactInfo the {@link PeerContactInfo} of the remote peer to check
     * @return {@code true} if the remote peer is already connected to or if a connection is pending, {@code false}
     * otherwise
     */
    private boolean isAlreadyConnectedOrPending(PeerContactInfo peerContactInfo) {
        return torrent.hasPeer(peerContactInfo) || !pendingContacts.add(peerContactInfo);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private boolean isPieceChecksumValid(int pieceIndex) {
        try {
            byte[] pieceBytes = repository.getPiece(torrent, pieceIndex);
            Sha1Hash expected = torrent.getPieceHash(pieceIndex);
            return Sha1Hash.of(pieceBytes).equals(expected);
        } catch (IOException e) {
            log(Level.ERROR, String.format("Failed to retrieve piece %d", pieceIndex), e);
            return false;
        }
    }

    @Override
    public void onAnnounceResponse(List<PeerResponse> peerResponses) {
        peerResponses.stream()
                .map(PeerResponse::toPeerContactInfo)
                .forEach(this::handleDiscoveredPeerContact);
    }

    @Override
    public void handlePeerDisconnected(PeerHandler peerHandler) {
        log(Level.DEBUG, String.format("Handling peer disconnected: %s", peerHandler.getPeerContactInfo()));
        workDispatcher.removePeerHandler(peerHandler);
        torrent.removePeer(peerHandler.getPeer());
    }

    @Override
    public void handlePeerChoked(PeerHandler peerHandler) {
        log(Level.DEBUG, String.format("Handling choked by remote: %s", peerHandler.getPeerContactInfo()));

        peerHandler.getAvailablePieces().stream()
                .map(pieceIndexToAvailablePeerHandlers::get)
                .forEach(availablePeerHandlers -> availablePeerHandlers.remove(peerHandler));

        workDispatcher.handlePeerChoked(peerHandler);
    }

    @Override
    public void handlePeerUnchoked(PeerHandler peerHandler) {
        log(Level.DEBUG, String.format("Handling unchoked by remote: %s", peerHandler.getPeerContactInfo()));

        peerHandler.getAvailablePieces().stream()
                .map(pieceIndexToAvailablePeerHandlers::get)
                .forEach(availablePeerHandlers -> availablePeerHandlers.add(peerHandler));

        workDispatcher.handlePeerUnchoked(peerHandler);
    }

    @Override
    public void handlePiecesAvailable(PeerHandler peerHandler, Set<Integer> pieceIndices) {
        log(Level.DEBUG, String.format("Handling %d pieces available", pieceIndices.size()));
        pieceIndices.forEach(pieceIndex -> pieceIndexToAvailablePeerHandlers
                .computeIfAbsent(pieceIndex, key -> new HashSet<>())
                .add(peerHandler));
        workDispatcher.handlePieceAvailable(peerHandler);
    }

    @Override
    public void handleBlockRequested(PeerHandler peerHandler, int pieceIndex, int offset, int length) {
        log(Level.DEBUG, String.format("Handling block requested (%d - %d) for piece %d", offset, offset + length,
                pieceIndex));
        byte[] data;
        try {
            data = repository.getBlock(torrent, pieceIndex, offset, length);
        } catch (IOException e) {
            log(Level.ERROR, String.format("Failed to retrieve block [%d - %d] for piece %d",
                    offset, offset + length, pieceIndex), e);
            return;
        }
        try {
            peerHandler.sendPiece(pieceIndex, offset, data);
        } catch (IOException e) {
            log(Level.ERROR, String.format("[%s] Failed to send block [%d - %d] for piece %d",
                    peerHandler.getPeerContactInfo(), offset, offset + length, pieceIndex), e);
        }
    }

    @Override
    public void handleDhtPortReceived(PeerHandler peerHandler, int port) {
        log(Level.DEBUG, String.format("Handling DHT port received from %s: %d", peerHandler.getPeerContactInfo(),
                port));
        InetSocketAddress address = new InetSocketAddress(peerHandler.getAddress(), port);
        listeners.forEach(listener -> listener.onDhtNodeDiscovered(address));
    }

    private void log(Level level, String message) {
        LOGGER.log(level, String.format("[%s] %s", torrent.getName(), message));
    }

    private void log(Level level, String message, Throwable throwable) {
        LOGGER.log(level, String.format("[%s] %s", torrent.getName(), message), throwable);
    }

    public interface Listener {

        void onDhtNodeDiscovered(InetSocketAddress address);
    }

    private class UnchokeTask extends PeriodicTask {

        private static final int MAX_UNCHOKED_PEERS = 3;

        private Set<PeerHandler> unchokedPeerHandlers = new HashSet<>();
        private PeerHandler optimisticUnchokedPeerHandler;
        private int iteration;

        public UnchokeTask(ScheduledExecutorService scheduledExecutorService) {
            super(scheduledExecutorService);
        }

        @Override
        public void run() {
            if (isThirdIteration()) {
                selectPeerToOptimisticUnchoke().ifPresent(this::processPeerToOptimisticUnchoke);
            }
            Set<PeerHandler> peersToUnchoke = selectPeersToUnchoke();
            processPeersToUnchoke(peersToUnchoke);
            unchokedPeerHandlers = peersToUnchoke;
            iteration = (iteration + 1) % 3;
        }

        private boolean isThirdIteration() {
            return iteration == 0;
        }

        private Set<PeerHandler> selectPeersToUnchoke() {
            return peerHandlers.stream()
                    .filter(this::isNotOptimisticUnchoke)
                    .filter(PeerHandler::isRemoteInterested)
                    .sorted(this::comparePeerHandlersByTransferRate)
                    .limit(MAX_UNCHOKED_PEERS)
                    .collect(Collectors.toSet());
        }

        /**
         * Compares two PeerHandlers based on their upload or download rates. If all pieces are verified, the upload
         * rate is used, otherwise the download rate is used. The PeerHandler with the higher rate is considered to be
         * "less than" the PeerHandler with the lower rate.
         *
         * @param peerHandler1 The first PeerHandler to compare
         * @param peerHandler2 The second PeerHandler to compare
         * @return A negative integer if peerHandler1 has a higher rate than peerHandler2,
         * a positive integer if peerHandler1 has a lower rate than peerHandler2,
         * or zero if both PeerHandlers have the same rate.
         */
        private int comparePeerHandlersByTransferRate(PeerHandler peerHandler1, PeerHandler peerHandler2) {
            if (torrent.isAllPiecesVerified()) {
                return Double.compare(peerHandler2.getUploadRate(), peerHandler1.getUploadRate());
            } else {
                return Double.compare(peerHandler2.getDownloadRate(), peerHandler1.getDownloadRate());
            }
        }

        private boolean isNotOptimisticUnchoke(PeerHandler peerHandler) {
            return peerHandler != optimisticUnchokedPeerHandler;
        }

        private void processPeersToUnchoke(Set<PeerHandler> peersToUnchoke) {
            unchokedPeerHandlers.stream()
                    .filter(Predicate.not(peersToUnchoke::contains))
                    .forEach(peerHandler -> {
                        try {
                            peerHandler.sendChoke();
                        } catch (IOException e) {
                            log(Level.ERROR, String.format("[%s] Failed to send choke",
                                    peerHandler.getPeerContactInfo()), e);
                        }
                    });
            peersToUnchoke.stream()
                    .filter(Predicate.not(unchokedPeerHandlers::contains))
                    .forEach(peerHandler -> {
                        try {
                            peerHandler.sendUnchoke();
                        } catch (IOException e) {
                            log(Level.ERROR, String.format("[%s] Failed to send unchoke",
                                    peerHandler.getPeerContactInfo()), e);
                        }
                    });
        }

        private Optional<PeerHandler> selectPeerToOptimisticUnchoke() {
            List<PeerHandler> peerHandlersCopy = new ArrayList<>(peerHandlers);
            Collections.shuffle(peerHandlersCopy);
            return peerHandlersCopy.stream()
                    .filter(PeerHandler::isRemoteChoked)
                    .findFirst();
        }

        private void processPeerToOptimisticUnchoke(PeerHandler peerHandler) {
            assert peerHandler != optimisticUnchokedPeerHandler;

            // choke the current optimistic unchoked peer
            if (optimisticUnchokedPeerHandler != null) {
                try {
                    optimisticUnchokedPeerHandler.sendChoke();
                } catch (IOException e) {
                    log(Level.ERROR, String.format("[%s] Failed to send choke", optimisticUnchokedPeerHandler
                            .getPeerContactInfo()), e);
                }
            }

            // unchoke the new optimistic unchoked peer
            try {
                peerHandler.sendUnchoke();
                optimisticUnchokedPeerHandler = peerHandler;
            } catch (IOException e) {
                log(Level.ERROR, String.format("[%s] Failed to send unchoke", peerHandler.getPeerContactInfo()), e);
            }
        }
    }

    private class WorkDispatcher extends BackgroundTask {

        private final LinkedBlockingQueue<PeerHandler> peerHandlersQueue = new LinkedBlockingQueue<>();
        private final Map<PeerHandler, Boolean> peerHandlerToShouldEnqueueOnCompletion = new HashMap<>();
        private final Set<PeerHandler> chokedPeerHandlers = new HashSet<>();
        private final Set<PeerHandler> noPieceToAssignPeerHandlers = new HashSet<>();

        @Override
        protected void execute() throws InterruptedException {
            PeerHandler peerHandler = peerHandlersQueue.take();
            assignWork(peerHandler);
        }

        public synchronized void addPeerHandler(PeerHandler peerHandler) {
            peerHandlerToShouldEnqueueOnCompletion.computeIfAbsent(peerHandler, key -> {
                chokedPeerHandlers.add(peerHandler);
                return false;
            });
        }

        public synchronized void removePeerHandler(PeerHandler peerHandler) {
            peerHandlerToShouldEnqueueOnCompletion.remove(peerHandler);
            noPieceToAssignPeerHandlers.remove(peerHandler);
            chokedPeerHandlers.remove(peerHandler);
            peerHandlersQueue.remove(peerHandler);
        }

        public synchronized void handlePeerUnchoked(PeerHandler peerHandler) {
            if (!isPeerHandlerRegistered(peerHandler)) {
                return;
            }
            chokedPeerHandlers.remove(peerHandler);

            if (noPieceToAssignPeerHandlers.contains(peerHandler)) {
                return;
            }
            enqueuePeerHandler(peerHandler);
        }

        public synchronized void handlePeerChoked(PeerHandler peerHandler) {
            if (isPeerHandlerRegistered(peerHandler)) {
                removePeerHandler(peerHandler);
                chokedPeerHandlers.add(peerHandler);
                noPieceToAssignPeerHandlers.remove(peerHandler);
            }
        }

        public synchronized void handlePieceAvailable(PeerHandler peerHandler) {
            if (!isPeerHandlerRegistered(peerHandler)) {
                return;
            }

            if (!noPieceToAssignPeerHandlers.contains(peerHandler)) {
                return;
            }

            noPieceToAssignPeerHandlers.remove(peerHandler);

            if (chokedPeerHandlers.contains(peerHandler)) {
                return;
            }

            enqueuePeerHandler(peerHandler);
        }

        private boolean isPeerHandlerRegistered(PeerHandler peerHandler) {
            return peerHandlerToShouldEnqueueOnCompletion.containsKey(peerHandler);
        }

        private void assignWork(PeerHandler peerHandler) {
            Optional<Block> blockToAssignOpt = getBlockToAssign(peerHandler);
            if (blockToAssignOpt.isEmpty()) {
                LOGGER.log(Level.DEBUG, "No block to assign to {0}", peerHandler.getPeerContactInfo());
                noPieceToAssignPeerHandlers.add(peerHandler);
                return;
            }
            Block block = blockToAssignOpt.get();

            try {
                assignWork(peerHandler, block);

                if (!peerHandler.isRequestQueueFull()) {
                    enqueuePeerHandler(peerHandler);
                } else {
                    peerHandlerToShouldEnqueueOnCompletion.put(peerHandler, true);
                }
            } catch (IOException e) {
                log(Level.ERROR, String.format("Failed to assign work to %s", peerHandler.getPeerContactInfo()), e);
                peerHandler.stop();
            }
        }

        private void assignWork(PeerHandler peerHandler, Block block) throws IOException {
            log(Level.DEBUG, String.format("Assigning %s to %s", block, peerHandler.getPeerContactInfo()));
            int pieceIndex = block.getPieceIndex();
            int blockIndex = block.getBlockIndex();
            int offset = block.getBlockIndex() * torrent.getBlockSize();
            int length = torrent.getBlockSize(block.getPieceIndex(), block.getBlockIndex());
            peerHandler.sendRequest(block.getPieceIndex(), offset, length)
                    .handle((data, throwable) -> {
                        if (throwable == null) {
                            handleBlockReceived(pieceIndex, offset, data);
                        } else {
                            log(Level.ERROR, String.format("Failed to receive block %d of piece %d from %s", blockIndex,
                                    pieceIndex, peerHandler.getPeerContactInfo()), throwable);
                            synchronized (pieceStateLock) {
                                torrent.setBlockNotRequested(pieceIndex, blockIndex);
                            }
                        }

                        if (shouldEnqueueOnCompletion(peerHandler)) {
                            peerHandlerToShouldEnqueueOnCompletion.put(peerHandler, false);
                            enqueuePeerHandler(peerHandler);
                        }

                        return Void.TYPE;
                    });
            torrent.setBlockRequested(pieceIndex, blockIndex);
        }

        private boolean shouldEnqueueOnCompletion(PeerHandler peerHandler) {
            return peerHandlerToShouldEnqueueOnCompletion.getOrDefault(peerHandler, false);
        }

        private synchronized Optional<Block> getBlockToAssign(PeerHandler peerHandler) {
            Optional<Integer> pieceIndexToAssignOpt = getPieceIndexToAssign(peerHandler);
            if (pieceIndexToAssignOpt.isEmpty()) {
                return Optional.empty();
            }
            int pieceIndex = pieceIndexToAssignOpt.get();
            int blockIndex = torrent.getMissingBlocks(pieceIndex).stream().findFirst().getAsInt();
            return Optional.of(new Block(pieceIndex, blockIndex));
        }

        private synchronized Optional<Integer> getPieceIndexToAssign(PeerHandler peerHandler) {
            return getRarestPartiallyMissingPieceIndexFromPeer(peerHandler)
                    .or(() -> getRarestCompletelyMissingPieceIndexFromPeer(peerHandler));
        }

        private Optional<Integer> getRarestPartiallyMissingPieceIndexFromPeer(PeerHandler peerHandler) {
            Set<Integer> availablePieces = peerHandler.getAvailablePieces();
            return torrent.getPartiallyMissingPiecesWithUnrequestedBlocks().stream()
                    .boxed()
                    .filter(availablePieces::contains)
                    .min(Comparator.comparingInt(this::getPieceAvailability));
        }

        private int getPieceAvailability(int pieceIndex) {
            return pieceIndexToAvailablePeerHandlers.get(pieceIndex).size();
        }

        private Optional<Integer> getRarestCompletelyMissingPieceIndexFromPeer(PeerHandler peerHandler) {
            Set<Integer> availablePieces = peerHandler.getAvailablePieces();
            return torrent.getCompletelyMissingPiecesWithUnrequestedBlocks().stream()
                    .boxed()
                    .filter(availablePieces::contains)
                    .min(Comparator.comparingInt(this::getPieceAvailability));
        }

        private void enqueuePeerHandler(PeerHandler peerHandler) {
            peerHandlersQueue.add(peerHandler);
        }

        public void handleBlockReceived(int pieceIndex, int offset, byte[] data) {
            log(Level.DEBUG, String.format("Handling %d bytes received for piece %d, offset %d", data.length,
                    pieceIndex, offset));

            int blockIndex = offset / torrent.getBlockSize();

            try {
                repository.storeBlock(torrent, pieceIndex, offset, data);
            } catch (IOException e) {
                log(Level.ERROR, String.format("Failed to store block %d of piece %d", blockIndex, pieceIndex), e);
                synchronized (pieceStateLock) {
                    torrent.setBlockNotRequested(pieceIndex, blockIndex);
                }
                return;
            }

            synchronized (pieceStateLock) {
                torrent.setBlockReceived(pieceIndex, blockIndex);
                torrent.setBlockNotRequested(pieceIndex, blockIndex);
                torrent.incrementDownloaded(data.length);

                if (torrent.isPieceComplete(pieceIndex)) {
                    LOGGER.log(Level.DEBUG, "Piece {0} complete", pieceIndex);

                    if (isPieceChecksumValid(pieceIndex)) {
                        synchronized (verificationLock) {
                            LOGGER.log(Level.INFO, "Piece {0} verified", pieceIndex);
                            torrent.setPieceVerified(pieceIndex);
                            peerHandlers.forEach(handler -> {
                                try {
                                    handler.sendHave(pieceIndex);
                                } catch (IOException e) {
                                    log(Level.ERROR, String.format("[%s] Failed to notify remote of piece availability",
                                            handler.getPeerContactInfo()), e);
                                }
                            });
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "Piece {0} verification failed", pieceIndex);
                        torrent.setPieceMissing(pieceIndex);
                        enqueueIdlePeerHandlersWithPiece(pieceIndex);
                    }
                }
            }

            if (torrent.isAllPiecesVerified()) {
                LOGGER.log(Level.DEBUG, "All pieces received");
                trackerHandlers.forEach(TrackerHandler::announceCompleted);
                peerHandlers.forEach(peerHandler -> {
                    try {
                        peerHandler.sendNotInterested();
                    } catch (IOException e) {
                        log(Level.ERROR, String.format("[%s] Failed to send not interested",
                                peerHandler.getPeerContactInfo()), e);
                    }
                });
                trackerHandlers.forEach(TrackerHandler::stop);
            }
        }

        private void enqueueIdlePeerHandlersWithPiece(int pieceIndex) {
            noPieceToAssignPeerHandlers.stream()
                    .filter(peer -> peer.getAvailablePieces().contains(pieceIndex))
                    .forEach(this::enqueuePeerHandler);
        }
    }
}
