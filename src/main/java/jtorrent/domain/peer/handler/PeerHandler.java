package jtorrent.domain.peer.handler;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jtorrent.domain.common.util.BackgroundTask;
import jtorrent.domain.common.util.PeriodicTask;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.peer.communication.PeerSocket;
import jtorrent.domain.peer.model.Peer;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.peer.model.message.KeepAlive;
import jtorrent.domain.peer.model.message.PeerMessage;
import jtorrent.domain.peer.model.message.typed.Bitfield;
import jtorrent.domain.peer.model.message.typed.Cancel;
import jtorrent.domain.peer.model.message.typed.Choke;
import jtorrent.domain.peer.model.message.typed.Have;
import jtorrent.domain.peer.model.message.typed.Interested;
import jtorrent.domain.peer.model.message.typed.NotInterested;
import jtorrent.domain.peer.model.message.typed.Piece;
import jtorrent.domain.peer.model.message.typed.Port;
import jtorrent.domain.peer.model.message.typed.Request;
import jtorrent.domain.peer.model.message.typed.TypedPeerMessage;
import jtorrent.domain.peer.model.message.typed.Unchoke;

public class PeerHandler {

    private static final Logger LOGGER = System.getLogger(PeerHandler.class.getName());
    private static final int MAX_REQUESTS = 5;
    private static final ExecutorService MESSAGE_HANDLER_THREAD_POOL = new ConnectionThreadPool();

    private final Peer peer;
    private final PeerSocket peerSocket;
    private final EventHandler eventHandler;
    private final Set<Integer> availablePieces = new HashSet<>();
    private final HandlePeerTask handlePeerTask;
    private final PeriodicKeepAliveTask periodicKeepAliveTask;
    private final PeriodicCheckAliveTask periodicCheckAliveTask;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final Map<RequestKey, CompletableFuture<byte[]>> outRequestKeyToFuture =
            new ConcurrentHashMap<>(MAX_REQUESTS);
    private final Map<RequestKey, Future<?>> inRequestKeyToFuture = new ConcurrentHashMap<>();

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

    public CompletableFuture<Boolean> connect(Sha1Hash infoHash, boolean isDhtSupported) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                peerSocket.connect(peer.getPeerContactInfo().toInetSocketAddress(), infoHash, isDhtSupported);
                return peerSocket.isDhtSupportedByRemote();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    public Peer getPeer() {
        return peer;
    }

    private void sendKeepAlive() throws IOException {
        sendMessage(new KeepAlive());
    }

    public void sendChoke() throws IOException {
        sendMessage(new Choke());
        peer.setRemoteChoked(true);
    }

    public void sendUnchoke() throws IOException {
        sendMessage(new Unchoke());
    }

    public void sendInterested() throws IOException {
        sendMessage(new Interested());
        peer.setLocalInterested(true);
    }

    public void sendNotInterested() throws IOException {
        sendMessage(new NotInterested());
        peer.setLocalInterested(false);
    }

    public void sendHave(int pieceIndex) throws IOException {
        Have have = new Have(pieceIndex);
        sendMessage(have);
    }

    public void sendBitfield(BitSet bitSet, int numTotalPieces) throws IOException {
        Bitfield bitfield = Bitfield.fromBitSetAndNumTotalPieces(bitSet, numTotalPieces);
        sendMessage(bitfield);
    }

    public CompletableFuture<byte[]> sendRequest(int index, int begin, int length) throws IOException {
        CompletableFuture<byte[]> future = new CompletableFuture<byte[]>().orTimeout(10, TimeUnit.SECONDS);
        RequestKey requestKey = new RequestKey(index, begin, length);
        future.whenComplete((result, throwable) -> outRequestKeyToFuture.remove(requestKey));
        outRequestKeyToFuture.put(requestKey, future);
        Request request = new Request(index, begin, length);
        sendMessage(request);
        return future;
    }

    public void sendPiece(int index, int begin, byte[] block) throws IOException {
        Piece piece = new Piece(index, begin, block);
        sendMessage(piece);
    }

    public void sendCancel(int index, int begin, int length) throws IOException {
        RequestKey requestKey = new RequestKey(index, begin, length);
        Future<?> future = inRequestKeyToFuture.remove(requestKey);
        if (future != null) {
            future.cancel(true);
        }
        Cancel cancel = new Cancel(index, begin, length);
        sendMessage(cancel);
    }

    public void sendPort(int port) throws IOException {
        Port portMessage = new Port(port);
        sendMessage(portMessage);
    }

    private void sendMessage(PeerMessage message) throws IOException {
        peerSocket.sendMessage(message);
        peer.addUploadedBytes(message.getMessageSize());
    }

    public double getDownloadRate() {
        return peer.getDownloadRate();
    }

    public double getUploadRate() {
        return peer.getUploadRate();
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
        return outRequestKeyToFuture.size() >= MAX_REQUESTS;
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

        void handlePeerDisconnected(PeerHandler peerHandler);

        void handlePeerChoked(PeerHandler peerHandler);

        void handlePeerUnchoked(PeerHandler peerHandler);

        void handlePiecesAvailable(PeerHandler peerHandler, Set<Integer> pieceIndices);

        void handleBlockRequested(PeerHandler peerHandler, int pieceIndex, int offset, int length);

        void handleDhtPortReceived(PeerHandler peerHandler, int port);
    }

    private static class RequestKey {

        private final int piece;
        private final int offset;
        private final int length;

        public RequestKey(int piece, int offset, int length) {
            this.piece = piece;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public int hashCode() {
            return Objects.hash(piece, offset, length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RequestKey that = (RequestKey) o;
            return piece == that.piece
                    && offset == that.offset
                    && length == that.length;
        }
    }

    private static class ConnectionThreadPool extends ThreadPoolExecutor {

        public ConnectionThreadPool() {
            super(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
                Thread thread = new Thread(r);
                thread.setName("ConnectionThreadPool-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            });
        }
    }

    private class HandlePeerTask extends BackgroundTask {

        @Override
        protected String getThreadName() {
            return "PeerHandler-" + peer.getPeerContactInfo();
        }

        @Override
        protected void execute() {
            try {
                PeerMessage message = peerSocket.receiveMessage();
                handleMessage(message);
            } catch (IOException e) {
                if (!isStopping()) {
                    LOGGER.log(Level.ERROR, String.format("[%s] Error while communicating with peer",
                            peer.getPeerContactInfo()), e);
                    PeerHandler.this.stop();
                }
            }
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
        }

        private void handleNotInterested() {
            peer.setRemoteInterested(false);
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
            RequestKey requestKey = new RequestKey(request.getIndex(), request.getBegin(), request.getLength());
            Future<?> future = MESSAGE_HANDLER_THREAD_POOL.submit(() -> {
                eventHandler.handleBlockRequested(PeerHandler.this, request.getIndex(), request.getBegin(),
                        request.getLength());
                inRequestKeyToFuture.remove(requestKey);
            });
            inRequestKeyToFuture.put(requestKey, future);
        }

        private void handlePiece(Piece piece) {
            RequestKey requestKey = new RequestKey(piece.getIndex(), piece.getBegin(), piece.getBlock().length);
            CompletableFuture<byte[]> future = outRequestKeyToFuture.remove(requestKey);

            if (future == null) {
                LOGGER.log(Level.ERROR, "[{0}] Received block that was not requested", peer.getPeerContactInfo());
                return;
            }

            future.complete(piece.getBlock());
        }

        private void handleCancel(Cancel cancel) {
            RequestKey requestKey = new RequestKey(cancel.getIndex(), cancel.getBegin(), cancel.getLength());
            Future<?> future = inRequestKeyToFuture.remove(requestKey);
            if (future != null) {
                LOGGER.log(Level.DEBUG, "[{0}] Cancelling request for {1}", peer.getPeerContactInfo(), requestKey);
                future.cancel(true);
            } else {
                LOGGER.log(Level.DEBUG, "[{0}] Failed to cancel request for {1} ", peer.getPeerContactInfo(),
                        requestKey);
            }
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
                sendKeepAlive();
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
