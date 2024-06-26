package jtorrent.domain.torrent.handler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jtorrent.domain.common.Constants;
import jtorrent.domain.common.util.BackgroundTask;
import jtorrent.domain.common.util.PeriodicTask;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.common.util.logging.MdcUtil;
import jtorrent.domain.peer.communication.PeerSocket;
import jtorrent.domain.peer.handler.PeerHandler;
import jtorrent.domain.peer.model.Peer;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.torrent.model.Block;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.repository.PieceRepository;
import jtorrent.domain.tracker.handler.TrackerHandler;
import jtorrent.domain.tracker.handler.factory.TrackerHandlerFactory;
import jtorrent.domain.tracker.model.PeerResponse;

public class TorrentHandler implements TrackerHandler.Listener, PeerHandler.EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentHandler.class);

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
    /**
     * Used to prevent concurrent modification to the state of the torrent.
     */
    private final Object stateLock = new Object();

    public TorrentHandler(Torrent torrent, PieceRepository pieceRepository) {
        this.torrent = requireNonNull(torrent);
        this.repository = requireNonNull(pieceRepository);

        trackerHandlers = torrent.getTrackers().stream()
                .map(tracker -> TrackerHandlerFactory.create(torrent, tracker))
                .collect(Collectors.toSet());
        trackerHandlers.forEach(trackerHandler -> trackerHandler.addListener(this));
    }

    public void start() {
        MdcUtil.putTorrent(torrent);
        Map<String, String> context = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> {
            MDC.setContextMap(context);
            verifyFiles();
        }).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                LOGGER.error(Markers.TORRENT, "Failed to start", throwable);
            } else {
                boolean isCompleted = torrent.isAllPiecesVerified();
                Torrent.State state = isCompleted ? Torrent.State.SEEDING : Torrent.State.DOWNLOADING;
                torrent.setState(state);
                workDispatcher.start();
                trackerHandlers.forEach(TrackerHandler::start);
                unchokeTask.scheduleAtFixedRate(0, 10, SECONDS);
                LOGGER.info(Markers.TORRENT, "Torrent started");
            }
            MDC.clear();
        });
        MdcUtil.removeTorrent();
    }

    public void stop() {
        MdcUtil.putTorrent(torrent);
        synchronized (stateLock) {
            torrent.setState(Torrent.State.STOPPED);
        }
        workDispatcher.stop();
        trackerHandlers.forEach(TrackerHandler::stop);
        unchokeTask.stop();
        executorService.shutdownNow();
        peerHandlers.forEach(PeerHandler::stop);
        torrent.clearPeers();
        MdcUtil.removeTorrent();
    }

    private void verifyFiles() {
        torrent.setState(Torrent.State.CHECKING);
        torrent.resetCheckedBytes();
        synchronized (pieceStateLock) {
            IntStream.range(0, torrent.getNumPieces())
                    .parallel()
                    .forEach(piece -> {
                        if (isPieceChecksumValid(piece)) {
                            torrent.setPieceVerified(piece);
                        } else {
                            torrent.setPieceMissing(piece);
                        }
                        torrent.setPieceChecked(piece);
                    });
        }
    }

    public void handleInboundPeerConnection(PeerSocket peerSocket) {
        MdcUtil.putTorrent(torrent);
        PeerContactInfo peerContactInfo = peerSocket.getPeerContactInfo();

        if (isAlreadyConnectedOrPending(peerContactInfo)) {
            LOGGER.debug(Markers.TORRENT, "Already connected or pending connection {}", peerContactInfo);
            return;
        }

        Peer peer = new Peer(peerSocket.getPeerContactInfo());
        PeerHandler peerHandler = new PeerHandler(peer, peerSocket, this);
        connectPeerHandler(peerHandler);
        MdcUtil.removeTorrent();
    }

    public void handleDiscoveredPeerContact(PeerContactInfo peerContactInfo) {
        MdcUtil.putTorrent(torrent);
        if (isAlreadyConnectedOrPending(peerContactInfo)) {
            LOGGER.debug(Markers.TORRENT, "Already connected or pending connection {}", peerContactInfo);
            return;
        }

        PeerSocket peerSocket = new PeerSocket();
        Peer peer = new Peer(peerContactInfo);
        PeerHandler peerHandler = new PeerHandler(peer, peerSocket, this);
        connectPeerHandler(peerHandler);
        MdcUtil.removeTorrent();
    }

    private void connectPeerHandler(PeerHandler peerHandler) {
        peerHandler.connect(torrent.getInfoHash(), true)
                .whenComplete((isDhtSupportedByRemote, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error(Markers.TORRENT, "Failed to connect to {}", peerHandler.getPeerContactInfo());
                    } else {
                        handleConnectionSuccess(peerHandler, isDhtSupportedByRemote);
                    }
                    pendingContacts.remove(peerHandler.getPeerContactInfo());
                });
    }

    private void handleConnectionSuccess(PeerHandler peerHandler, boolean isDhtSupportedByRemote) {
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
            LOGGER.error(Markers.TORRENT, "Failed to connect to {}", peerHandler.getPeerContactInfo(), e);
        }
        LOGGER.info(Markers.TORRENT, "Connected to {}", peerHandler.getPeerContactInfo());
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
            LOGGER.error(Markers.TORRENT, "Failed to retrieve piece {}", pieceIndex, e);
            return false;
        }
    }

    @Override
    public void onAnnounceResponse(List<PeerResponse> peerResponses) {
        MdcUtil.putTorrent(torrent);
        peerResponses.stream()
                .map(PeerResponse::toPeerContactInfo)
                .forEach(this::handleDiscoveredPeerContact);
        MdcUtil.removeTorrent();
    }

    @Override
    public void handlePeerDisconnected(PeerHandler peerHandler) {
        MdcUtil.putTorrent(torrent);
        LOGGER.info(Markers.TORRENT, "Peer disconnected: {}", peerHandler.getPeerContactInfo());
        workDispatcher.removePeerHandler(peerHandler);
        torrent.removePeer(peerHandler.getPeer());
        peerHandlers.remove(peerHandler);
        MdcUtil.removeTorrent();
    }

    @Override
    public void handlePeerChoked(PeerHandler peerHandler) {
        MdcUtil.putTorrent(torrent);
        LOGGER.info(Markers.TORRENT, "Choked by peer {}", peerHandler.getPeerContactInfo());

        peerHandler.getAvailablePieces().stream()
                .map(pieceIndexToAvailablePeerHandlers::get)
                .forEach(availablePeerHandlers -> availablePeerHandlers.remove(peerHandler));

        workDispatcher.handlePeerChoked(peerHandler);
        MdcUtil.removeTorrent();
    }

    @Override
    public void handlePeerUnchoked(PeerHandler peerHandler) {
        MdcUtil.putTorrent(torrent);
        LOGGER.info(Markers.TORRENT, "Unchoked by peer {}", peerHandler.getPeerContactInfo());

        peerHandler.getAvailablePieces().stream()
                .map(pieceIndexToAvailablePeerHandlers::get)
                .forEach(availablePeerHandlers -> availablePeerHandlers.add(peerHandler));

        workDispatcher.handlePeerUnchoked(peerHandler);
        MdcUtil.removeTorrent();
    }

    @Override
    public void handlePiecesAvailable(PeerHandler peerHandler, Set<Integer> pieceIndices) {
        MdcUtil.putTorrent(torrent);
        LOGGER.info(Markers.TORRENT, "Peer {} has {} pieces available", peerHandler.getPeerContactInfo(),
                pieceIndices.size());
        pieceIndices.forEach(pieceIndex -> pieceIndexToAvailablePeerHandlers
                .computeIfAbsent(pieceIndex, key -> new HashSet<>())
                .add(peerHandler));
        workDispatcher.handlePieceAvailable(peerHandler);
        MdcUtil.removeTorrent();
    }

    @Override
    public void handleBlockRequested(PeerHandler peerHandler, int pieceIndex, int offset, int length) {
        MdcUtil.putTorrent(torrent);
        LOGGER.info(Markers.TORRENT, "Peer requested block ({}, {}) for piece {}", offset, offset + length, pieceIndex);
        byte[] data;
        try {
            data = repository.getBlock(torrent, pieceIndex, offset, length);
        } catch (IOException e) {
            LOGGER.error(Markers.TORRENT, "Failed to retrieve block ({}, {}) for piece {}", offset, offset + length,
                    pieceIndex, e);
            return;
        }
        try {
            peerHandler.sendPiece(pieceIndex, offset, data);
            LOGGER.info(Markers.TORRENT, "Sent block ({}, {}) for piece {}", offset, offset + length, pieceIndex);
            torrent.incrementUploaded(data.length);
        } catch (IOException e) {
            LOGGER.error(Markers.TORRENT, "Failed to send block ({}, {}) for piece {}", offset, offset + length,
                    pieceIndex, e);
        }
        MdcUtil.removeTorrent();
    }

    @Override
    public void handleDhtPortReceived(PeerHandler peerHandler, int port) {
        MdcUtil.putTorrent(torrent);
        LOGGER.info(Markers.TORRENT, "Peer {} supports DHT on port {}", peerHandler.getPeerContactInfo(), port);
        InetSocketAddress address = new InetSocketAddress(peerHandler.getAddress(), port);
        listeners.forEach(listener -> listener.onDhtNodeDiscovered(address));
        MdcUtil.removeTorrent();
    }

    public Torrent getTorrent() {
        return torrent;
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
            MdcUtil.putTorrent(torrent);
            if (isThirdIteration()) {
                selectPeerToOptimisticUnchoke().ifPresent(this::processPeerToOptimisticUnchoke);
            }
            Set<PeerHandler> peersToUnchoke = selectPeersToUnchoke();
            processPeersToUnchoke(peersToUnchoke);
            unchokedPeerHandlers = peersToUnchoke;
            iteration = (iteration + 1) % 3;
            MdcUtil.removeTorrent();
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
                            LOGGER.error(Markers.TORRENT, "Failed to send choke to {}",
                                    peerHandler.getPeerContactInfo(), e);
                        }
                    });
            peersToUnchoke.stream()
                    .filter(Predicate.not(unchokedPeerHandlers::contains))
                    .forEach(peerHandler -> {
                        try {
                            peerHandler.sendUnchoke();
                        } catch (IOException e) {
                            LOGGER.error(Markers.TORRENT, "Failed to send unchoke to {}",
                                    peerHandler.getPeerContactInfo(), e);
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
                    LOGGER.error(Markers.TORRENT, "Failed to send choke to {}",
                            optimisticUnchokedPeerHandler.getPeerContactInfo(), e);
                }
            }

            // unchoke the new optimistic unchoked peer
            try {
                peerHandler.sendUnchoke();
                optimisticUnchokedPeerHandler = peerHandler;
            } catch (IOException e) {
                LOGGER.error(Markers.TORRENT, "Failed to send unchoke to {}", peerHandler.getPeerContactInfo(), e);
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

        @Override
        protected void doOnStarted() {
            MdcUtil.putTorrent(torrent);
            LOGGER.debug(Markers.TORRENT, "Work dispatcher started");
        }

        @Override
        protected void doOnStopped() {
            LOGGER.debug(Markers.TORRENT, "Work dispatcher stopped");
            MdcUtil.removeTorrent();
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
                LOGGER.debug("No block to assign to {}", peerHandler.getPeerContactInfo());
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
                LOGGER.error(Markers.TORRENT, "Failed to assign work to {}", peerHandler.getPeerContactInfo(), e);
                peerHandler.stop();
            }
        }

        private void assignWork(PeerHandler peerHandler, Block block) throws IOException {
            LOGGER.debug("Assigned {} to {}", block, peerHandler.getPeerContactInfo());
            int pieceIndex = block.getPieceIndex();
            int blockIndex = block.getBlockIndex();
            int offset = block.getBlockIndex() * torrent.getBlockSize();
            int length = torrent.getBlockSize(block.getPieceIndex(), block.getBlockIndex());
            peerHandler.sendRequest(block.getPieceIndex(), offset, length)
                    .handle((data, throwable) -> {
                        MdcUtil.putTorrent(torrent);
                        if (throwable == null) {
                            handleBlockReceived(pieceIndex, offset, data);
                        } else {
                            LOGGER.error(Markers.TORRENT, "Failed to receive block {} of piece {} from {}",
                                    blockIndex, pieceIndex, peerHandler.getPeerContactInfo(), throwable);
                            synchronized (pieceStateLock) {
                                torrent.setBlockNotRequested(pieceIndex, blockIndex);
                            }
                        }

                        if (shouldEnqueueOnCompletion(peerHandler)) {
                            peerHandlerToShouldEnqueueOnCompletion.put(peerHandler, false);
                            enqueuePeerHandler(peerHandler);
                        }

                        MdcUtil.removeTorrent();
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
            LOGGER.info(Markers.TORRENT, "Received {} bytes for piece {}, offset {}", data.length, pieceIndex, offset);

            int blockIndex = offset / torrent.getBlockSize();

            try {
                repository.storeBlock(torrent, pieceIndex, offset, data);
            } catch (IOException e) {
                LOGGER.error(Markers.TORRENT, "Failed to store block {} of piece {}", blockIndex, pieceIndex, e);
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
                    LOGGER.info(Markers.TORRENT, "Piece {} complete", pieceIndex);

                    if (isPieceChecksumValid(pieceIndex)) {
                        synchronized (verificationLock) {
                            LOGGER.info(Markers.TORRENT, "Piece {} verified", pieceIndex);
                            torrent.setPieceVerified(pieceIndex);
                            peerHandlers.forEach(handler -> {
                                try {
                                    handler.sendHave(pieceIndex);
                                } catch (IOException e) {
                                    LOGGER.error(Markers.TORRENT, "Failed to send have to {}",
                                            handler.getPeerContactInfo(), e);

                                }
                            });
                        }
                    } else {
                        LOGGER.error(Markers.TORRENT, "Piece {} verification failed", pieceIndex);
                        torrent.setPieceMissing(pieceIndex);
                        enqueueIdlePeerHandlersWithPiece(pieceIndex);
                    }
                }
            }

            if (torrent.isAllPiecesVerified()) {
                LOGGER.info(Markers.TORRENT, "All pieces verified");
                trackerHandlers.forEach(TrackerHandler::announceCompleted);
                peerHandlers.forEach(peerHandler -> {
                    try {
                        peerHandler.sendNotInterested();
                    } catch (IOException e) {
                        LOGGER.error(Markers.TORRENT, "Failed to send not interested to {}",
                                peerHandler.getPeerContactInfo(), e);
                    }
                });
                trackerHandlers.forEach(TrackerHandler::stop);
                synchronized (stateLock) {
                    // we need to do this check in a synchronized block because the torrent could have been stopped
                    // concurrently right before we set the state. Without this check, the state could be set to
                    // STOPPED and then to SEEDING right after.
                    if (torrent.getState() == Torrent.State.DOWNLOADING) {
                        torrent.setState(Torrent.State.SEEDING);
                    }
                }
            }
        }

        private void enqueueIdlePeerHandlersWithPiece(int pieceIndex) {
            noPieceToAssignPeerHandlers.stream()
                    .filter(peer -> peer.getAvailablePieces().contains(pieceIndex))
                    .forEach(this::enqueuePeerHandler);
        }
    }
}
