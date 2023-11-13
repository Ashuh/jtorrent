package jtorrent.peer.domain.handler;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jtorrent.common.domain.model.Block;
import jtorrent.common.domain.util.BackgroundTask;
import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.peer.domain.communication.PeerSocket;
import jtorrent.peer.domain.model.Peer;
import jtorrent.peer.domain.model.message.KeepAlive;
import jtorrent.peer.domain.model.message.PeerMessage;
import jtorrent.peer.domain.model.message.typed.Bitfield;
import jtorrent.peer.domain.model.message.typed.Cancel;
import jtorrent.peer.domain.model.message.typed.Choke;
import jtorrent.peer.domain.model.message.typed.Have;
import jtorrent.peer.domain.model.message.typed.Interested;
import jtorrent.peer.domain.model.message.typed.NotInterested;
import jtorrent.peer.domain.model.message.typed.Piece;
import jtorrent.peer.domain.model.message.typed.Port;
import jtorrent.peer.domain.model.message.typed.Request;
import jtorrent.peer.domain.model.message.typed.TypedPeerMessage;
import jtorrent.peer.domain.model.message.typed.Unchoke;

public class PeerHandler {

    private static final Logger LOGGER = System.getLogger(PeerHandler.class.getName());

    private final Peer peer;
    private final PeerSocket peerSocket;
    private final Sha1Hash infoHash;
    private final List<Listener> listeners = new ArrayList<>();
    private final Set<Integer> availablePieces = new HashSet<>();
    private final HandlePeerTask handlePeerTask;

    private boolean isBusy = false;

    public PeerHandler(Peer peer, PeerSocket peerSocket, Sha1Hash infoHash) {
        this.peerSocket = peerSocket;
        this.peer = peer;
        this.infoHash = infoHash;
        handlePeerTask = new HandlePeerTask();
    }

    public void start() {
        handlePeerTask.start();
    }

    public void stop() {
        try {
            peerSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "[{0}] Error while closing socket", peer.getPeerContactInfo());
        }
        handlePeerTask.stop();
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
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

    @Override
    public String toString() {
        return "PeerHandler{"
                + "peer=" + peer
                + ", infoHash=" + infoHash
                + ", isBusy=" + isBusy
                + '}';
    }

    public interface Listener {

        void onUnchokeRecevied(PeerHandler peerHandler);

        void onChokeReceived(PeerHandler peerHandler);

        void onReady(PeerHandler peerHandler);

        void onPieceReceived(Piece piece);

        void onPieceAvailable(PeerHandler peerHandler, int index);

        void onPortReceived(PeerHandler peerHandler, int port);
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
                // TODO: hardcoded true for now
                peerSocket.connect(infoHash, true);
                sendInterested();
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[{0}] Error while connecting to peer", peer.getPeerContactInfo());
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

        public void handleKeepAlive() {
        }

        public void handleChoke() {
            peer.setLocalChoked(true);
            listeners.forEach(listener -> listener.onChokeReceived(PeerHandler.this));
        }

        public void handleUnchoke() {
            peer.setLocalChoked(false);
            listeners.forEach(listener -> listener.onUnchokeRecevied(PeerHandler.this));
            notifyIfReady();
        }

        private void notifyIfReady() {
            if (!isReady()) {
                return;
            }

            listeners.forEach(listener -> listener.onReady(PeerHandler.this));
        }

        public void handleInterested() {
        }

        public void handleNotInterested() {
        }

        public void handleHave(Have have) {
            int pieceIndex = have.getPieceIndex();
            availablePieces.add(pieceIndex);
            listeners.forEach(listener -> listener.onPieceAvailable(PeerHandler.this, pieceIndex));
        }

        public void handleBitfield(Bitfield bitfield) {
            bitfield.getBits()
                    .forEach(i -> {
                        availablePieces.add(i);
                        listeners.forEach(listener -> listener.onPieceAvailable(PeerHandler.this, i));
                    });
        }

        public void handleRequest(Request request) {
        }

        public void handlePiece(Piece piece) {
            isBusy = false;
            listeners.forEach(listener -> listener.onPieceReceived(piece));
            notifyIfReady();
        }

        public void handleCancel(Cancel cancel) {
        }

        public void handlePort(Port port) {
            listeners.forEach(listener -> listener.onPortReceived(PeerHandler.this, port.getListenPort()));
        }
    }
}
