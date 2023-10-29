package jtorrent.domain.handler.torrent;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level;
import static java.lang.System.getLogger;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

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
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jtorrent.data.repository.FilePieceRepository;
import jtorrent.domain.handler.peer.PeerHandler;
import jtorrent.domain.handler.tracker.TrackerHandler;
import jtorrent.domain.handler.tracker.factory.TrackerHandlerFactory;
import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.model.peer.PeerContactInfo;
import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.torrent.Block;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.PeerResponse;
import jtorrent.domain.repository.PieceRepository;
import jtorrent.domain.socket.PeerSocket;
import jtorrent.domain.util.BackgroundTask;
import jtorrent.domain.util.Sha1Hash;

public class TorrentHandler implements TrackerHandler.Listener, PeerHandler.Listener {

    private static final Logger LOGGER = getLogger(TorrentHandler.class.getName());

    private final Torrent torrent;
    private final Set<TrackerHandler> trackerHandlers;
    private final Set<PeerHandler> peerHandlers = new HashSet<>();
    private final Map<Integer, Set<PeerHandler>> pieceIndexToAvailablePeerHandlers = new HashMap<>();
    private final WorkDispatcher workDispatcher = new WorkDispatcher();
    private final PieceRepository repository = new FilePieceRepository();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final List<Listener> listeners = new ArrayList<>();

    public TorrentHandler(Torrent torrent) {
        this.torrent = requireNonNull(torrent);

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
        peerHandlers.forEach(PeerHandler::stop);
        torrent.clearPeers();
    }

    public void addPeer(PeerSocket peerSocket) {
        PeerContactInfo peerContactInfo =
                new PeerContactInfo(peerSocket.getRemoteAddress(), peerSocket.getRemotePort());

        if (torrent.hasPeer(peerContactInfo)) {
            return;
        }

        Peer peer = new Peer(peerContactInfo);
        torrent.addPeer(peer);
        PeerHandler peerHandler = new PeerHandler(peer, peerSocket, torrent);
        peerHandler.addListener(this);
        peerHandlers.add(peerHandler);
        peerHandler.start();
    }

    public void addPeer(PeerContactInfo peerContactInfo) {
        if (torrent.hasPeer(peerContactInfo)) {
            return;
        }

        Peer peer = new Peer(peerContactInfo);
        torrent.addPeer(peer);

        try {
            PeerSocket peerSocket = new PeerSocket(peerContactInfo);
            PeerHandler peerHandler = new PeerHandler(peer, peerSocket, torrent);
            peerHandler.addListener(this);
            peerHandlers.add(peerHandler);
            peerHandler.start();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Failed to connect to peer: {0}", peerContactInfo);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void onUnchokeRecevied(PeerHandler peerHandler) {
        LOGGER.log(Level.DEBUG, "Unchoked received: {0}", peerHandler);

        peerHandler.getAvailablePieces().stream()
                .map(pieceIndexToAvailablePeerHandlers::get)
                .forEach(availablePeerHandlers -> availablePeerHandlers.add(peerHandler));
    }

    @Override
    public void onChokeReceived(PeerHandler peerHandler) {
        LOGGER.log(Level.DEBUG, "Choke received: {0}", peerHandler);

        peerHandler.getAvailablePieces().stream()
                .map(pieceIndexToAvailablePeerHandlers::get)
                .forEach(availablePeerHandlers -> availablePeerHandlers.remove(peerHandler));
    }

    @Override
    public void onReady(PeerHandler peerHandler) {
        LOGGER.log(Level.DEBUG, "PeerHandler ready: {0}", peerHandler);
        workDispatcher.addPeerHandler(peerHandler);
    }

    @Override
    public void onPieceReceived(Piece piece) {
        LOGGER.log(Level.DEBUG, "Piece received: {0}", piece);

        int pieceIndex = piece.getIndex();
        int blockIndex = piece.getBegin() / torrent.getBlockSize();

        repository.storeBlock(torrent, pieceIndex, piece.getBegin(), piece.getBlock());

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

        torrent.incrementDownloaded(piece.getBlock().length);

        if (torrent.isAllPiecesVerified()) {
            LOGGER.log(Level.DEBUG, "All pieces received");
            trackerHandlers.forEach(TrackerHandler::stop);
        }
    }

    @Override
    public void onPieceAvailable(PeerHandler peerHandler, int index) {
        pieceIndexToAvailablePeerHandlers
                .computeIfAbsent(index, key -> new HashSet<>())
                .add(peerHandler);

        if (peerHandler.isReady()) {
            workDispatcher.addPeerHandler(peerHandler);
        }
    }

    @Override
    public void onPortReceived(PeerHandler peerHandler, int port) {
        InetSocketAddress address = new InetSocketAddress(peerHandler.getAddress(), port);
        listeners.forEach(listener -> listener.onDhtNodeDiscovered(address));
    }

    @Override
    public void onAnnounceResponse(List<PeerResponse> peerResponses) {
        peerResponses.stream()
                .map(PeerResponse::toPeerContactInfo)
                .filter(Predicate.not(torrent::hasPeer))
                .forEach(this::addPeer);
    }

    public interface Listener {

        void onDhtNodeDiscovered(InetSocketAddress address);
    }

    private class WorkDispatcher extends BackgroundTask {

        private final LinkedBlockingQueue<PeerHandler> peerHandlersQueue = new LinkedBlockingQueue<>();

        @Override
        protected void execute() throws InterruptedException {
            PeerHandler peerHandler = peerHandlersQueue.take();
            assignBlock(peerHandler);
        }

        private void assignBlock(PeerHandler peerHandler) {
            getPieceIndexToAssign(peerHandler).ifPresentOrElse(pieceIndex -> {
                int blockIndex = torrent.getmissingBlockIndices(pieceIndex).iterator().next();
                Block block = createBlock(pieceIndex, blockIndex);
                try {
                    peerHandler.assignBlock(block);
                    torrent.setBlockRequested(pieceIndex, blockIndex);
                } catch (IOException e) {
                    WorkDispatcher.this.stop();
                }
            }, () -> LOGGER.log(Level.DEBUG, "No block to assign to {0}", peerHandler));
        }

        public Optional<Integer> getPieceIndexToAssign(PeerHandler peerHandler) {
            Optional<Integer> rarestPartiallyMissingPieceIndex =
                    getRarestPartiallyMissingPieceIndexFromPeer(peerHandler);

            if (rarestPartiallyMissingPieceIndex.isPresent()) {
                return rarestPartiallyMissingPieceIndex;
            }

            return getRarestCompletelyMissingPieceIndexFromPeer(peerHandler);
        }

        private Block createBlock(int pieceIndex, int blockIndex) {
            int blockOffset = blockIndex * torrent.getBlockSize();
            int blockSize = torrent.getBlockSize(pieceIndex, blockIndex);
            return new Block(pieceIndex, blockOffset, blockSize);
        }

        private Optional<Integer> getRarestPartiallyMissingPieceIndexFromPeer(PeerHandler peerHandler) {
            Set<Integer> availablePieces = peerHandler.getAvailablePieces();
            return torrent.getPartiallyMissingPieceIndices().stream()
                    .filter(availablePieces::contains)
                    .min(Comparator.comparingInt(this::getPieceAvailability));
        }

        private Optional<Integer> getRarestCompletelyMissingPieceIndexFromPeer(PeerHandler peerHandler) {
            Set<Integer> availablePieces = peerHandler.getAvailablePieces();
            return torrent.getCompletelyMissingPieceIndices().stream()
                    .filter(availablePieces::contains)
                    .min(Comparator.comparingInt(this::getPieceAvailability));
        }

        public void addPeerHandler(PeerHandler peerHandler) {
            if (peerHandlersQueue.contains(peerHandler)) {
                return;
            }
            peerHandlersQueue.add(peerHandler);
        }

        private int getPieceAvailability(int pieceIndex) {
            return pieceIndexToAvailablePeerHandlers.get(pieceIndex).size();
        }
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
}
