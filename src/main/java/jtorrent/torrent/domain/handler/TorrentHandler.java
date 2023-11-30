package jtorrent.torrent.domain.handler;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level;
import static java.lang.System.getLogger;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jtorrent.common.domain.model.Block;
import jtorrent.common.domain.util.BackgroundTask;
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
    private final ExecutorService peerConnectionExecutor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    /**
     * Set of peer contacts that are currently being connected to.
     */
    private final Set<PeerContactInfo> pendingContacts = ConcurrentHashMap.newKeySet();
    private final List<Listener> listeners = new ArrayList<>();

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
        workDispatcher.start();
        trackerHandlers.forEach(TrackerHandler::start);
        executorService.scheduleAtFixedRate(new Unchoke(peerHandlers), 0, 10, SECONDS);
        executorService.scheduleAtFixedRate(new OptimisticUnchoke(peerHandlers), 0, 30, SECONDS);
    }

    public void stop() {
        torrent.setIsActive(false);
        workDispatcher.stop();
        trackerHandlers.forEach(TrackerHandler::stop);
        executorService.shutdownNow();
        peerConnectionExecutor.shutdownNow();
        peerHandlers.forEach(PeerHandler::stop);
        torrent.clearPeers();
    }

    public void handleInboundPeerConnection(PeerSocket peerSocket) {
        PeerContactInfo peerContactInfo = peerSocket.getPeerContactInfo();

        if (isAlreadyConnectedOrPending(peerContactInfo)) {
            LOGGER.log(Level.DEBUG, "[{0}] Already connected or pending connection {0}", peerContactInfo);
            return;
        }

        peerConnectionExecutor.execute(() -> {
            try {
                peerSocket.acceptInboundConnection(true);
                LOGGER.log(Level.INFO, "Connected to {0}", peerContactInfo);
                addNewPeerConnection(peerSocket);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Failed to connect to peer: {0}", peerContactInfo);
            } finally {
                pendingContacts.remove(peerContactInfo);
            }
        });
    }

    public void handleDiscoveredPeerContact(PeerContactInfo peerContactInfo) {
        if (isAlreadyConnectedOrPending(peerContactInfo)) {
            LOGGER.log(Level.DEBUG, "[{0}] Already connected or pending connection {0}", peerContactInfo);
            return;
        }

        peerConnectionExecutor.execute(() -> {
            try {
                PeerSocket peerSocket = new PeerSocket();
                peerSocket.connect(peerContactInfo.toInetSocketAddress(), torrent.getInfoHash(), true);
                LOGGER.log(Level.INFO, "Connected to {0}", peerContactInfo);
                addNewPeerConnection(peerSocket);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Failed to connect to peer: {0}", peerContactInfo);
            } finally {
                pendingContacts.remove(peerContactInfo);
            }
        });
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

    private void addNewPeerConnection(PeerSocket peerSocket) {
        Peer peer = new Peer(peerSocket.getPeerContactInfo());
        torrent.addPeer(peer);
        PeerHandler peerHandler = new PeerHandler(peer, peerSocket, this);
        peerHandlers.add(peerHandler);
        peerHandler.start();
        workDispatcher.addPeerHandler(peerHandler);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void onAnnounceResponse(List<PeerResponse> peerResponses) {
        peerResponses.stream()
                .map(PeerResponse::toPeerContactInfo)
                .forEach(this::handleDiscoveredPeerContact);
    }

    @Override
    public void handlePeerConnected(PeerHandler peerHandler) {
        log(Level.DEBUG, String.format("Handling peer connected: %s", peerHandler.getPeerContactInfo()));
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
    public void handlePeerInterested(PeerHandler peerHandler) {
        log(Level.DEBUG, String.format("Handling peer interested: %s", peerHandler.getPeerContactInfo()));
    }

    @Override
    public void handlePeerNotInterested(PeerHandler peerHandler) {
        log(Level.DEBUG, String.format("Handling peer not interested: %s", peerHandler.getPeerContactInfo()));
    }

    @Override
    public void handlePiecesAvailable(PeerHandler peerHandler, Set<Integer> pieceIndices) {
        log(Level.DEBUG, String.format("Handling %d pieces available", pieceIndices.size()));
        pieceIndices.forEach(pieceIndex -> pieceIndexToAvailablePeerHandlers
                .computeIfAbsent(pieceIndex, key -> new HashSet<>())
                .add(peerHandler));
        workDispatcher.handlePieceAvailable(peerHandler);
    }

    public void handleBlockReceived(int pieceIndex, int offset, byte[] data) {
        log(Level.DEBUG, String.format("Handling %d bytes received for piece %d, offset %d", data.length, pieceIndex,
                offset));

        int blockIndex = offset / torrent.getBlockSize();

        repository.storeBlock(torrent, pieceIndex, offset, data);

        torrent.setBlockReceived(pieceIndex, blockIndex);

        if (torrent.isPieceComplete(pieceIndex)) {
            LOGGER.log(Level.DEBUG, "Piece {0} complete", pieceIndex);

            byte[] pieceBytes = repository.getPiece(torrent, pieceIndex);
            Sha1Hash expected = torrent.getPieceHashes().get(pieceIndex);

            if (Sha1Hash.of(pieceBytes).equals(expected)) {
                LOGGER.log(Level.INFO, "Piece {0} verified", pieceIndex);
                torrent.setPieceVerified(pieceIndex);
            } else {
                LOGGER.log(Level.WARNING, "Piece {0} verification failed", pieceIndex);
                torrent.setPieceMissing(pieceIndex);
            }
        }

        torrent.incrementDownloaded(data.length);

        if (torrent.isAllPiecesVerified()) {
            LOGGER.log(Level.DEBUG, "All pieces received");
            trackerHandlers.forEach(TrackerHandler::stop);
        }
    }

    @Override
    public void handleBlockRequested(PeerHandler peerHandler, int pieceIndex, int offset, int length) {
        log(Level.DEBUG, String.format("Handling block requested (%d - %d) for piece %d", offset, offset + length,
                pieceIndex));
    }

    @Override
    public void handleBlockCancelled(PeerHandler peerHandler, int pieceIndex, int offset, int length) {
        log(Level.DEBUG, String.format("Handling block cancelled (%d - %d) for piece %d", offset, offset + length,
                pieceIndex));
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

    public interface Listener {

        void onDhtNodeDiscovered(InetSocketAddress address);
    }

    private static class Unchoke implements Runnable {

        private static final int MAX_UNCHOKED_PEERS = 4;

        private final Collection<PeerHandler> peerHandlers;

        private Set<PeerHandler> unchokedPeerHandlers = new HashSet<>();

        private Unchoke(Collection<PeerHandler> peerHandlers) {
            this.peerHandlers = peerHandlers;
        }

        @Override
        public void run() {
            Set<PeerHandler> toUnchoke = peerHandlers.stream()
                    .filter(PeerHandler::isRemoteInterested)
                    .sorted(Comparator.comparingDouble(PeerHandler::getDownloadRate).reversed())
                    .limit(MAX_UNCHOKED_PEERS)
                    .collect(Collectors.toSet());

            unchokedPeerHandlers.stream()
                    .filter(Predicate.not(toUnchoke::contains))
                    .forEach(PeerHandler::choke);

            toUnchoke.stream()
                    .filter(Predicate.not(unchokedPeerHandlers::contains))
                    .forEach(PeerHandler::unchoke);

            unchokedPeerHandlers = toUnchoke;
        }
    }

    private static class OptimisticUnchoke implements Runnable {

        private final Collection<PeerHandler> peerHandlers;
        private PeerHandler optimisticUnchokedPeerHandler;

        private OptimisticUnchoke(Collection<PeerHandler> peerHandlers) {
            this.peerHandlers = peerHandlers;
        }

        @Override
        public void run() {
            List<PeerHandler> peerHandlersCopy = new ArrayList<>(this.peerHandlers);
            Collections.shuffle(peerHandlersCopy);
            peerHandlersCopy.stream()
                    .filter(PeerHandler::isRemoteChoked)
                    .findFirst()
                    .ifPresent(this::setOptimisticUnchokedPeerHandler);
        }

        private void setOptimisticUnchokedPeerHandler(PeerHandler peerHandler) {
            assert peerHandler != optimisticUnchokedPeerHandler;

            if (optimisticUnchokedPeerHandler != null) {
                optimisticUnchokedPeerHandler.choke();
            }

            optimisticUnchokedPeerHandler = peerHandler;
            optimisticUnchokedPeerHandler.unchoke();
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
            LOGGER.log(Level.ERROR, "Removing peer handler from queue: {0}", peerHandler);
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
            Optional<Integer> pieceIndexToAssignOpt;

            synchronized (this) {
                pieceIndexToAssignOpt = getPieceIndexToAssign(peerHandler);
                if (pieceIndexToAssignOpt.isEmpty()) {
                    LOGGER.log(Level.DEBUG, "No piece to assign to {0}", peerHandler);
                    noPieceToAssignPeerHandlers.add(peerHandler);
                    return;
                }
            }

            int pieceIndex = pieceIndexToAssignOpt.get();
            LOGGER.log(Level.DEBUG, "Assigning piece to {0}", peerHandler);
            int blockIndex = torrent.getmissingBlockIndices(pieceIndex).iterator().next();
            try {
                assignWork(peerHandler, pieceIndex, blockIndex);

                if (!peerHandler.isRequestQueueFull()) {
                    enqueuePeerHandler(peerHandler);
                } else {
                    peerHandlerToShouldEnqueueOnCompletion.put(peerHandler, true);
                }
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Error occurred while assigning work", e);
                WorkDispatcher.this.stop();
            }
        }

        private void assignWork(PeerHandler peerHandler, int pieceIndex, int blockIndex) throws IOException {
            Block block = createBlock(pieceIndex, blockIndex);
            peerHandler.assignBlock(block)
                    .thenAccept(data -> handleBlockReceived(pieceIndex, block.getOffset(), data))
                    .whenComplete((unused, throwable) -> {
                        if (throwable != null) {
                            LOGGER.log(Level.ERROR, "Error occurred while assigning block", throwable);
                            torrent.setBlockMissing(pieceIndex, blockIndex);
                        }
                        if (shouldEnqueueOnCompletion(peerHandler)) {
                            peerHandlerToShouldEnqueueOnCompletion.put(peerHandler, false);
                            enqueuePeerHandler(peerHandler);
                        }
                    });
            torrent.setBlockRequested(pieceIndex, blockIndex);
        }

        private boolean shouldEnqueueOnCompletion(PeerHandler peerHandler) {
            return peerHandlerToShouldEnqueueOnCompletion.getOrDefault(peerHandler, false);
        }

        public Optional<Integer> getPieceIndexToAssign(PeerHandler peerHandler) {
            return getRarestPartiallyMissingPieceIndexFromPeer(peerHandler)
                    .or(() -> getRarestCompletelyMissingPieceIndexFromPeer(peerHandler));
        }

        private Optional<Integer> getRarestPartiallyMissingPieceIndexFromPeer(PeerHandler peerHandler) {
            Set<Integer> availablePieces = peerHandler.getAvailablePieces();
            return torrent.getPartiallyMissingPieceIndices().stream()
                    .filter(availablePieces::contains)
                    .min(Comparator.comparingInt(this::getPieceAvailability));
        }

        private int getPieceAvailability(int pieceIndex) {
            return pieceIndexToAvailablePeerHandlers.get(pieceIndex).size();
        }

        private Optional<Integer> getRarestCompletelyMissingPieceIndexFromPeer(PeerHandler peerHandler) {
            Set<Integer> availablePieces = peerHandler.getAvailablePieces();
            return torrent.getCompletelyMissingPieceIndices().stream()
                    .filter(availablePieces::contains)
                    .min(Comparator.comparingInt(this::getPieceAvailability));
        }

        private Block createBlock(int pieceIndex, int blockIndex) {
            int blockOffset = blockIndex * torrent.getBlockSize();
            int blockSize = torrent.getBlockSize(pieceIndex, blockIndex);
            return new Block(pieceIndex, blockOffset, blockSize);
        }

        private void enqueuePeerHandler(PeerHandler peerHandler) {
            LOGGER.log(Level.ERROR, "Adding peer handler to queue: {0}", peerHandler);
            peerHandlersQueue.add(peerHandler);
        }
    }
}
