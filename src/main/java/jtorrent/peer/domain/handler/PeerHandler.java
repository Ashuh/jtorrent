package jtorrent.peer.domain.handler;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jtorrent.common.domain.model.Block;
import jtorrent.common.domain.util.BackgroundTask;
import jtorrent.common.domain.util.PeriodicTask;
import jtorrent.peer.domain.communication.PeerSocket;
import jtorrent.peer.domain.model.Peer;
import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.peer.domain.model.message.KeepAlive;
import jtorrent.peer.domain.model.message.PeerMessage;
import jtorrent.peer.domain.model.message.typed.Bitfield;
import jtorrent.peer.domain.model.message.typed.Cancel;
import jtorrent.peer.domain.model.message.typed.Have;
import jtorrent.peer.domain.model.message.typed.Piece;
import jtorrent.peer.domain.model.message.typed.Port;
import jtorrent.peer.domain.model.message.typed.Request;
import jtorrent.peer.domain.model.message.typed.TypedPeerMessage;

public class PeerHandler {

    private static final Logger LOGGER = System.getLogger(PeerHandler.class.getName());
    private static final int MAX_REQUESTS = 5;

    private final Peer peer;
    private final PeerSocket peerSocket;
    private final EventHandler eventHandler;
    private final Set<Integer> availablePieces = new HashSet<>();
    private final HandlePeerTask handlePeerTask;
    private final PeriodicKeepAliveTask periodicKeepAliveTask;
    private final PeriodicCheckAliveTask periodicCheckAliveTask;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final Map<Block, CompletableFuture<byte[]>> blockToFuture = new ConcurrentHashMap<>(MAX_REQUESTS);

    public PeerHandler(Peer peer, PeerSocket peerSocket, EventHandler eventHandler) {
        this.peerSocket = peerSocket;
        this.peer = peer;
        this.eventHandler = eventHandler;
        handlePeerTask = new HandlePeerTask();
        periodicKeepAliveTask = new PeriodicKeepAliveTask(scheduledExecutorService);
        periodicCheckAliveTask = new PeriodicCheckAliveTask(scheduledExecutorService);
        peer.setLastSeenNow();
    }

    public void start() {
        handlePeerTask.start();
        periodicKeepAliveTask.scheduleAtFixedRate(2, TimeUnit.MINUTES);
        periodicCheckAliveTask.scheduleAtFixedRate(2, TimeUnit.MINUTES);
    }

    public void stop() {
        eventHandler.handlePeerDisconnected(this);
        try {
            peerSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "[{0}] Error while closing socket", peer.getPeerContactInfo());
        }
        handlePeerTask.stop();
        periodicKeepAliveTask.stop();
        periodicCheckAliveTask.stop();
        scheduledExecutorService.shutdownNow();
    }

    public Peer getPeer() {
        return peer;
    }

    public CompletableFuture<byte[]> assignBlock(Block block) throws IOException {
        LOGGER.log(Level.DEBUG, "[{0}] Assigned block", peer.getPeerContactInfo(), block);

        CompletableFuture<byte[]> future = new CompletableFuture<byte[]>().orTimeout(5, TimeUnit.SECONDS);
        future.whenComplete((result, throwable) -> blockToFuture.remove(block));

        blockToFuture.put(block, future);
        int index = block.getIndex();
        int offset = block.getOffset();
        int length = block.getLength();
        peerSocket.sendRequest(index, offset, length);
        return future;
    }

    public void choke() {
        LOGGER.log(Level.INFO, "[{0}] Choking remote", peer.getPeerContactInfo());

        try {
            peerSocket.sendChoke();
            peer.setRemoteChoked(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unchoke() {
        LOGGER.log(Level.INFO, "[{0}] Unchoking remote", peer.getPeerContactInfo());

        try {
            peerSocket.sendUnchoke();
            peer.setRemoteChoked(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public double getDownloadRate() {
        return peer.getDownloadRate();
    }

    public Set<Integer> getAvailablePieces() {
        return availablePieces;
    }

    public boolean isRemoteChoked() {
        return peer.isRemoteChoked();
    }

    public boolean isRemoteInterested() {
        return peer.isRemoteInterested();
    }

    public boolean isRequestQueueFull() {
        return blockToFuture.size() >= MAX_REQUESTS;
    }

    public InetAddress getAddress() {
        return peer.getAddress();
    }

    public PeerContactInfo getPeerContactInfo() {
        return peer.getPeerContactInfo();
    }

    @Override
    public String toString() {
        return "PeerHandler{"
                + "peer=" + peer
                + '}';
    }

    public interface EventHandler {

        void handlePeerConnected(PeerHandler peerHandler);

        void handlePeerDisconnected(PeerHandler peerHandler);

        void handlePeerChoked(PeerHandler peerHandler);

        void handlePeerUnchoked(PeerHandler peerHandler);

        void handlePeerInterested(PeerHandler peerHandler);

        void handlePeerNotInterested(PeerHandler peerHandler);

        void handlePiecesAvailable(PeerHandler peerHandler, Set<Integer> pieceIndices);

        void handleBlockRequested(PeerHandler peerHandler, int pieceIndex, int offset, int length);

        void handleBlockCancelled(PeerHandler peerHandler, int pieceIndex, int offset, int length);

        void handleDhtPortReceived(PeerHandler peerHandler, int port);
    }

    private class HandlePeerTask extends BackgroundTask {

        @Override
        protected Optional<String> getThreadName() {
            return Optional.of("PeerHandler-" + peer.getPeerContactInfo());
        }

        @Override
        protected void execute() {
            try {
                PeerMessage message = peerSocket.receiveMessage();
                handleMessage(message);
            } catch (IOException e) {
                if (!isStopping()) {
                    LOGGER.log(Level.ERROR, "[{0}] Error while communicating with peer", peer.getPeerContactInfo());
                    PeerHandler.this.stop();
                }
            }
        }

        @Override
        protected void doOnStarted() {
            try {
                peerSocket.sendInterested();
                peer.setLocalInterested(true);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[{0}] Error sending Interested", peer.getPeerContactInfo());
                PeerHandler.this.stop();
            }
        }

        @Override
        protected void doOnStop() {
            peer.disconnect();
        }

        private void handleMessage(PeerMessage message) {
            LOGGER.log(Level.DEBUG, "[{0}] Handling {1}", peer.getPeerContactInfo(), message);

            peer.addDownloadedBytes(message.getMessageSize());
            peer.setLastSeenNow();

            if (message instanceof KeepAlive) {
                return;
            }

            assert message instanceof TypedPeerMessage;
            TypedPeerMessage typedMessage = (TypedPeerMessage) message;

            switch (typedMessage.getMessageType()) {
            case CHOKE:
                handleChoke();
                return;
            case UNCHOKE:
                handleUnchoke();
                return;
            case INTERESTED:
                handleInterested();
                return;
            case NOT_INTERESTED:
                handleNotInterested();
                return;
            case HAVE:
                handleHave((Have) typedMessage);
                return;
            case BITFIELD:
                handleBitfield((Bitfield) typedMessage);
                return;
            case REQUEST:
                handleRequest((Request) typedMessage);
                return;
            case PIECE:
                handlePiece((Piece) typedMessage);
                return;
            case CANCEL:
                handleCancel((Cancel) typedMessage);
                return;
            case PORT:
                handlePort((Port) typedMessage);
                return;
            default:
                throw new AssertionError("Unknown message type: " + typedMessage.getMessageType());
            }
        }

        private void handleChoke() {
            peer.setLocalChoked(true);
            eventHandler.handlePeerChoked(PeerHandler.this);
        }

        private void handleUnchoke() {
            peer.setLocalChoked(false);
            eventHandler.handlePeerUnchoked(PeerHandler.this);
        }

        private void handleInterested() {
            peer.setRemoteInterested(true);
            eventHandler.handlePeerInterested(PeerHandler.this);
        }

        private void handleNotInterested() {
            peer.setRemoteInterested(false);
            eventHandler.handlePeerNotInterested(PeerHandler.this);
        }

        private void handleHave(Have have) {
            int pieceIndex = have.getPieceIndex();
            availablePieces.add(pieceIndex);
            eventHandler.handlePiecesAvailable(PeerHandler.this, Set.of(pieceIndex));
        }

        private void handleBitfield(Bitfield bitfield) {
            Set<Integer> newAvailablePieces = new HashSet<>();
            bitfield.getBits().forEach(newAvailablePieces::add);
            availablePieces.addAll(newAvailablePieces);
            eventHandler.handlePiecesAvailable(PeerHandler.this, newAvailablePieces);
        }

        private void handleRequest(Request request) {
            eventHandler.handleBlockRequested(PeerHandler.this, request.getIndex(), request.getBegin(),
                    request.getLength());
        }

        private void handlePiece(Piece piece) {
            Block block = new Block(piece.getIndex(), piece.getBegin(), piece.getBlock().length);
            CompletableFuture<byte[]> future = blockToFuture.remove(block);

            if (future == null) {
                LOGGER.log(Level.ERROR, "[{0}] Received block that was not requested", peer.getPeerContactInfo());
                return;
            }

            future.complete(piece.getBlock());
        }

        private void handleCancel(Cancel cancel) {
            eventHandler.handleBlockCancelled(PeerHandler.this, cancel.getIndex(), cancel.getBegin(),
                    cancel.getLength());
        }

        private void handlePort(Port port) {
            eventHandler.handleDhtPortReceived(PeerHandler.this, port.getListenPort());
        }
    }

    private class PeriodicKeepAliveTask extends PeriodicTask {

        protected PeriodicKeepAliveTask(ScheduledExecutorService scheduledExecutorService) {
            super(scheduledExecutorService);
        }

        @Override
        public void run() {
            try {
                peerSocket.sendKeepAlive();
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[{0}] Error sending KeepAlive", peer.getPeerContactInfo());
                PeerHandler.this.stop();
            }
        }
    }

    private class PeriodicCheckAliveTask extends PeriodicTask {

        protected PeriodicCheckAliveTask(ScheduledExecutorService scheduledExecutorService) {
            super(scheduledExecutorService);
        }

        @Override
        public void run() {
            if (isPeerAlive()) {
                return;
            }

            LOGGER.log(Level.INFO, "[{0}] Peer is dead, stopping", peer.getPeerContactInfo());
            PeerHandler.this.stop();
        }

        private boolean isPeerAlive() {
            return peer.isLastSeenWithin(Duration.of(2, ChronoUnit.MINUTES));
        }
    }
}
