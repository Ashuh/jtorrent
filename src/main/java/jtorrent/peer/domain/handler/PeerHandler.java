package jtorrent.peer.domain.handler;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
import jtorrent.peer.domain.model.message.typed.Choke;
import jtorrent.peer.domain.model.message.typed.Have;
import jtorrent.peer.domain.model.message.typed.Interested;
import jtorrent.peer.domain.model.message.typed.Piece;
import jtorrent.peer.domain.model.message.typed.Port;
import jtorrent.peer.domain.model.message.typed.Request;
import jtorrent.peer.domain.model.message.typed.TypedPeerMessage;
import jtorrent.peer.domain.model.message.typed.Unchoke;

public class PeerHandler {

    private static final Logger LOGGER = System.getLogger(PeerHandler.class.getName());

    private final Peer peer;
    private final PeerSocket peerSocket;
    private final EventHandler eventHandler;
    private final Set<Integer> availablePieces = new HashSet<>();
    private final HandlePeerTask handlePeerTask;
    private final PeriodicKeepAliveTask periodicKeepAliveTask;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private boolean isBusy = false;

    public PeerHandler(Peer peer, PeerSocket peerSocket, EventHandler eventHandler) {
        this.peerSocket = peerSocket;
        this.peer = peer;
        this.eventHandler = eventHandler;
        handlePeerTask = new HandlePeerTask();
        periodicKeepAliveTask = new PeriodicKeepAliveTask(scheduledExecutorService);
    }

    public void start() {
        handlePeerTask.start();
        periodicKeepAliveTask.scheduleAtFixedRate(2, TimeUnit.MINUTES);
    }

    public void stop() {
        try {
            peerSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "[{0}] Error while closing socket", peer.getPeerContactInfo());
        }
        handlePeerTask.stop();
        periodicKeepAliveTask.stop();
        scheduledExecutorService.shutdownNow();
    }

    public void assignBlock(Block block) throws IOException {
        LOGGER.log(Level.DEBUG, "[{0}] Assigned block", peer.getPeerContactInfo(), block);

        isBusy = true;
        int index = block.getIndex();
        int offset = block.getOffset();
        int length = block.getLength();
        sendRequest(index, offset, length);
    }

    private void sendRequest(int index, int begin, int length) throws IOException {
        Request request = new Request(index, begin, length);
        peerSocket.sendMessage(request);
    }

    private void sendInterested() throws IOException {
        Interested interested = new Interested();
        peerSocket.sendMessage(interested);
    }

    public void choke() {
        LOGGER.log(Level.INFO, "[{0}] Choking remote", peer.getPeerContactInfo());

        Choke choke = new Choke();
        try {
            peerSocket.sendMessage(choke);
            peer.setRemoteChoked(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unchoke() {
        LOGGER.log(Level.INFO, "[{0}] Unchoking remote", peer.getPeerContactInfo());

        Unchoke unchoke = new Unchoke();
        try {
            peerSocket.sendMessage(unchoke);
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

    public boolean isReady() {
        return peerSocket.isConnected() && !peer.isLocalChoked() && !isBusy;
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
                + ", isBusy=" + isBusy
                + '}';
    }

    public interface EventHandler {

        void onReady(PeerHandler peerHandler);

        void handlePeerConnected(PeerHandler peerHandler);

        void handlePeerDisconnected(PeerHandler peerHandler);

        void handlePeerChoked(PeerHandler peerHandler);

        void handlePeerUnchoked(PeerHandler peerHandler);

        void handlePeerInterested(PeerHandler peerHandler);

        void handlePeerNotInterested(PeerHandler peerHandler);

        void handlePieceAvailable(PeerHandler peerHandler, int pieceIndex);

        void handlePiecesAvailable(PeerHandler peerHandler, Set<Integer> pieceIndices);

        void handleBlockReceived(PeerHandler peerHandler, int pieceIndex, int offset, byte[] data);

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
                    HandlePeerTask.this.stop();
                }
            }
        }

        @Override
        protected void doOnStarted() {
            try {
                sendInterested();
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[{0}] Error sending Interested", peer.getPeerContactInfo());
                super.stop();
            }
        }

        @Override
        protected void doOnStop() {
            peer.disconnect();
        }

        private void handleMessage(PeerMessage message) {
            LOGGER.log(Level.DEBUG, "[{0}] Handling {1}", peer.getPeerContactInfo(), message);

            peer.addDownloadedBytes(message.getMessageSize());

            if (message instanceof KeepAlive) {
                handleKeepAlive();
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

        private void handleKeepAlive() {
        }

        private void handleChoke() {
            peer.setLocalChoked(true);
            eventHandler.handlePeerChoked(PeerHandler.this);
        }

        private void handleUnchoke() {
            peer.setLocalChoked(false);
            eventHandler.handlePeerUnchoked(PeerHandler.this);
            notifyIfReady();
        }

        private void notifyIfReady() {
            if (!isReady()) {
                return;
            }

            eventHandler.onReady(PeerHandler.this);
        }

        private void handleInterested() {
            eventHandler.handlePeerInterested(PeerHandler.this);
        }

        private void handleNotInterested() {
            eventHandler.handlePeerNotInterested(PeerHandler.this);
        }

        private void handleHave(Have have) {
            int pieceIndex = have.getPieceIndex();
            availablePieces.add(pieceIndex);
            eventHandler.handlePieceAvailable(PeerHandler.this, pieceIndex);
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
            isBusy = false;
            eventHandler.handleBlockReceived(PeerHandler.this, piece.getIndex(), piece.getBegin(), piece.getBlock());
            notifyIfReady();
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
                PeriodicKeepAliveTask.this.stop();
            }
        }
    }
}
