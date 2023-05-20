package jtorrent.domain.handler;

import static java.lang.System.Logger;
import static java.lang.System.Logger.Level;
import static java.lang.System.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.torrent.Block;
import jtorrent.domain.model.torrent.Sha1Hash;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.udp.UdpTracker;
import jtorrent.domain.model.tracker.udp.message.PeerResponse;

public class TorrentHandler implements UdpTrackerHandler.Listener, PeerHandler.Listener {

    private static final Logger LOGGER = getLogger(TorrentHandler.class.getName());

    private final Torrent torrent;
    private final Set<UdpTrackerHandler> trackerHandlers;
    private final Set<PeerHandler> peerHandlers = new HashSet<>();
    private final Set<Block> remainingBlocks = new HashSet<>();
    private final LinkedBlockingQueue<Block> workQueue = new LinkedBlockingQueue<>();
    private final Map<Integer, Set<PeerHandler>> pieceIndexToAvailablePeerHandlers = new HashMap<>();

    private final ByteBuffer buffer;

    public TorrentHandler(Torrent torrent) {
        this.torrent = requireNonNull(torrent);
        buffer = ByteBuffer.allocate(torrent.getTotalSize());

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
        byte[] data = piece.getBlock();
        int offset = pieceIndex * torrent.getPieceSize() + piece.getBegin();

        LOGGER.log(Level.TRACE, "Writing {0} bytes at offset {1}", data.length, offset);
        buffer.position(offset);
        buffer.put(data);

        int from = piece.getBegin();
        int to = from + piece.getBlock().length;
        torrent.setDataReceived(pieceIndex, from, to);

        if (torrent.isPieceComplete(pieceIndex)) {
            LOGGER.log(Level.DEBUG, "Piece {0} complete", pieceIndex);

            byte[] pieceBytes = new byte[torrent.getPieceSize(pieceIndex)];
            buffer.position(pieceIndex * torrent.getPieceSize());
            buffer.get(pieceBytes);

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
}
