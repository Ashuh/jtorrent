package jtorrent.domain.handler;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jtorrent.data.repository.FilePieceRepository;
import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.torrent.Block;
import jtorrent.domain.model.torrent.Sha1Hash;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.udp.UdpTracker;
import jtorrent.domain.model.tracker.udp.message.PeerResponse;
import jtorrent.domain.repository.PieceRepository;

public class TorrentHandler implements UdpTrackerHandler.Listener, PeerHandler.Listener {

    private static final Logger LOGGER = getLogger(TorrentHandler.class.getName());

    private final Torrent torrent;
    private final Set<UdpTrackerHandler> trackerHandlers;
    private final Set<PeerHandler> peerHandlers = new HashSet<>();
    private final Set<Block> remainingBlocks = new HashSet<>();
    private final LinkedBlockingQueue<Block> workQueue = new LinkedBlockingQueue<>();
    private final Map<Integer, Set<PeerHandler>> pieceIndexToAvailablePeerHandlers = new HashMap<>();
    private final PieceRepository repository = new FilePieceRepository();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public TorrentHandler(Torrent torrent) {
        this.torrent = requireNonNull(torrent);

        for (int i = 0; i < torrent.getNumPieces(); i++) {
            this.remainingBlocks.add(new Block(i, 0, torrent.getPieceSize(i)));
        }

        this.trackerHandlers = torrent.getTrackers().stream()
                .map(x -> new InetSocketAddress(x.getHost(), x.getPort()))
                .map(UdpTracker::new)
                .map(x -> new UdpTrackerHandler(x, torrent))
                .collect(Collectors.toSet());

        WorkDispatcher dispatcher = new WorkDispatcher();
        Thread dispatcherThread = new Thread(dispatcher);
        dispatcherThread.start();
        executorService.scheduleAtFixedRate(new Unchoke(peerHandlers), 0, 10, SECONDS);
        executorService.scheduleAtFixedRate(new OptimisticUnchoke(peerHandlers), 0, 30, SECONDS);
    }

    public void start() {
        trackerHandlers.forEach(trackerHandler -> trackerHandler.addListener(this));
        trackerHandlers.forEach(UdpTrackerHandler::start);
    }

    private void addBlockToWorkQueue(Block block) {
        if (remainingBlocks.contains(block)
                && !workQueue.contains(block)
                && !pieceIndexToAvailablePeerHandlers.get(block.getIndex()).isEmpty()) {
            workQueue.add(block);
        }
    }

    @Override
    public void onReady(PeerHandler peerHandler) {
        LOGGER.log(Level.DEBUG, "PeerHandler ready: {0}", peerHandler);

        peerHandler.getAvailablePieces().stream()
                .map(pieceIndexToAvailablePeerHandlers::get)
                .forEach(availablePeerHandlers -> availablePeerHandlers.add(peerHandler));

        peerHandler.getAvailablePieces()
                .stream()
                .map(index -> new Block(index, 0, torrent.getPieceSize(index)))
                .filter(remainingBlocks::contains) // only add blocks that are missing
                .forEach(workQueue::add);
    }

    @Override
    public void onPieceReceived(Piece piece) {
        LOGGER.log(Level.DEBUG, "Piece received: {0}", piece);

        Block block = new Block(piece.getIndex(), piece.getBegin(), piece.getBlock().length);
        remainingBlocks.remove(block);

        int pieceIndex = piece.getIndex();
        repository.storeBlock(torrent, pieceIndex, piece.getBegin(), piece.getBlock());

        int from = piece.getBegin();
        int to = from + piece.getBlock().length;
        torrent.setDataReceived(pieceIndex, from, to);

        if (torrent.isPieceComplete(pieceIndex)) {
            LOGGER.log(Level.DEBUG, "Piece {0} complete", pieceIndex);

            byte[] pieceBytes = repository.getPiece(torrent, pieceIndex);
            Sha1Hash expected = torrent.getPieceHashes().get(pieceIndex);

            if (Sha1Hash.of(pieceBytes).equals(expected)) {
                LOGGER.log(Level.INFO, "Piece {0} verified", pieceIndex);
                torrent.setPieceVerified(pieceIndex);
            } else {
                LOGGER.log(Level.WARNING, "Piece {0} verification failed", pieceIndex);
                torrent.unsetDataReceived(pieceIndex, from, to);
                Block newBlock = new Block(pieceIndex, 0, torrent.getPieceSize(pieceIndex));
                addBlockToWorkQueue(newBlock);
                remainingBlocks.add(newBlock);
            }
        }

        torrent.incrementDownloaded(piece.getBlock().length);

        if (remainingBlocks.isEmpty()) {
            LOGGER.log(Level.DEBUG, "All pieces received");
            trackerHandlers.forEach(UdpTrackerHandler::stop);
        }
    }

    @Override
    public void onPieceAvailable(PeerHandler peerHandler, int index) {
        pieceIndexToAvailablePeerHandlers
                .computeIfAbsent(index, key -> new HashSet<>())
                .add(peerHandler);

        int pieceSize = torrent.getPieceSize(index);
        Block block = new Block(index, 0, pieceSize);

        if (workQueue.contains(block)) {
            return;
        }

        workQueue.add(new Block(index, 0, pieceSize));
    }

    private int getPieceAvailability(int pieceIndex) {
        return pieceIndexToAvailablePeerHandlers.get(pieceIndex).size();
    }

    @Override
    public void onAnnounceResponse(List<PeerResponse> peerResponses) {
        peerResponses.parallelStream()
                .map(PeerResponse::toPeer)
                .map(peer -> new PeerHandler(peer, torrent))
                .filter(peerHandlers::add)
                .forEach(peerHandler -> {
                    peerHandler.addListener(this);
                    Thread thread = new Thread(peerHandler);
                    thread.start();
                });
    }

    private class WorkDispatcher implements Runnable {

        private boolean isRunning = true;

        @Override
        public void run() {
            while (isRunning) {
                try {
                    List<Block> blocks = drainAtLeastOne();
                    blocks.forEach(this::assignWork);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private List<Block> drainAtLeastOne() throws InterruptedException {
            List<Block> blocks = new ArrayList<>();
            blocks.add(workQueue.take());
            workQueue.drainTo(blocks);
            return blocks;
        }

        private void assignWork(Block block) {
            LOGGER.log(Level.DEBUG, "Assigning block: {0}", block);

            Set<PeerHandler> availablePeerHandlers = pieceIndexToAvailablePeerHandlers.get(block.getIndex());
            Queue<PeerHandler> peerHandlerQueue = new LinkedList<>(availablePeerHandlers);
            int size = availablePeerHandlers.size();

            for (int i = 0; i < size; i++) {
                PeerHandler peerHandler = peerHandlerQueue.poll();
                assert peerHandler != null;

                if (!peerHandler.isReady()) {
                    LOGGER.log(Level.DEBUG, "PeerHandler not ready: {0}", peerHandler);
                    continue;
                }

                try {
                    peerHandler.assignBlock(block);
                    LOGGER.log(Level.DEBUG, "Block assigned: {0}", block);
                    return;
                } catch (IOException e) {
                    // assume peer is no longer available
                    pieceIndexToAvailablePeerHandlers.get(block.getIndex()).remove(peerHandler);
                }
            }

            LOGGER.log(Level.DEBUG, "Block not assigned: {0}", block);
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
