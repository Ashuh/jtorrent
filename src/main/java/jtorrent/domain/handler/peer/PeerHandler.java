package jtorrent.domain.handler.peer;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.model.peer.message.KeepAlive;
import jtorrent.domain.model.peer.message.PeerMessage;
import jtorrent.domain.model.peer.message.typed.Bitfield;
import jtorrent.domain.model.peer.message.typed.Cancel;
import jtorrent.domain.model.peer.message.typed.Choke;
import jtorrent.domain.model.peer.message.typed.Have;
import jtorrent.domain.model.peer.message.typed.Interested;
import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.peer.message.typed.Port;
import jtorrent.domain.model.peer.message.typed.Request;
import jtorrent.domain.model.peer.message.typed.TypedPeerMessage;
import jtorrent.domain.model.peer.message.typed.Unchoke;
import jtorrent.domain.model.torrent.Block;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.socket.PeerSocket;
import jtorrent.domain.util.BackgroundTask;

public class PeerHandler {

    private static final Logger LOGGER = System.getLogger(PeerHandler.class.getName());

    private final Peer peer;
    private final PeerSocket peerSocket;
    private final Torrent torrent;
    private final List<Listener> listeners = new ArrayList<>();
    private final Set<Integer> availablePieces = new HashSet<>();
    private final HandlePeerTask handlePeerTask = new HandlePeerTask();

    private boolean isBusy = false;

    public PeerHandler(Peer peer, PeerSocket peerSocket, Torrent torrent) {
        this.peerSocket = peerSocket;
        this.peer = peer;
        this.torrent = torrent;
    }

    public void start() {
        handlePeerTask.start();
    }

    public void stop() {
        try {
            peerSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Error while closing socket", e);
        }
        handlePeerTask.stop();
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void assignBlock(Block block) throws IOException {
        LOGGER.log(Level.DEBUG, "Peer {0} assigned block {1}", peer, block);

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
        LOGGER.log(Level.DEBUG, "Choking peer: " + peer.getPeerContactInfo());
        Choke choke = new Choke();
        try {
            peerSocket.sendMessage(choke);
            peer.setRemoteChoked(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unchoke() {
        LOGGER.log(Level.DEBUG, "Unchoking peer: " + peer.getPeerContactInfo());
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
                + ", torrent=" + torrent
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
        protected void execute() {
            try {
                PeerMessage message = peerSocket.receiveMessage();
                handleMessage(message);
            } catch (IOException e) {
                if (!isStopping()) {
                    LOGGER.log(Level.ERROR, "Error while communicating with peer {0}", peer);
                    HandlePeerTask.this.stop();
                }
            }
        }

        @Override
        protected void doOnStarted() {
            try {
                // TODO: hardcoded true for now
                peerSocket.connect(torrent.getInfoHash(), true);
                sendInterested();
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Error while connecting to peer {0}", peer);
                super.stop();
            }
        }

        @Override
        protected void doOnStop() {
            peer.disconnect();
        }

        private void handleMessage(PeerMessage message) {
            LOGGER.log(Level.INFO, "Received message {0} from {1}", message, peer.getPeerContactInfo());

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
            LOGGER.log(Level.DEBUG, "Handling KeepAlive");
        }

        public void handleChoke() {
            LOGGER.log(Level.DEBUG, "Handling Choke");
            peer.setLocalChoked(true);
            listeners.forEach(listener -> listener.onChokeReceived(PeerHandler.this));
        }

        public void handleUnchoke() {
            LOGGER.log(Level.DEBUG, "Handling Unchoke");
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
            LOGGER.log(Level.DEBUG, "Handling Interested");
        }

        public void handleNotInterested() {
            LOGGER.log(Level.DEBUG, "Handling NotInterested");
        }

        public void handleHave(Have have) {
            LOGGER.log(Level.DEBUG, "Handling Have: {0}", have);
            int pieceIndex = have.getPieceIndex();
            availablePieces.add(pieceIndex);
            listeners.forEach(listener -> listener.onPieceAvailable(PeerHandler.this, pieceIndex));
        }

        public void handleBitfield(Bitfield bitfield) {
            LOGGER.log(Level.DEBUG, "Handling Bitfield: {0}", bitfield);
            bitfield.getBits()
                    .forEach(i -> {
                        availablePieces.add(i);
                        listeners.forEach(listener -> listener.onPieceAvailable(PeerHandler.this, i));
                    });
        }

        public void handleRequest(Request request) {
            LOGGER.log(Level.DEBUG, "Handling Request: {0}", request);
        }

        public void handlePiece(Piece piece) {
            LOGGER.log(Level.DEBUG, "Handling piece: {0}", piece);
            isBusy = false;
            listeners.forEach(listener -> listener.onPieceReceived(piece));
            peer.addDownloadedBytes(piece.getBlock().length);
            notifyIfReady();
        }

        public void handleCancel(Cancel cancel) {
            LOGGER.log(Level.DEBUG, "Handling Cancel: {0}", cancel);
        }

        public void handlePort(Port port) {
            listeners.forEach(listener -> listener.onPortReceived(PeerHandler.this, port.getListenPort()));
        }
    }
}
