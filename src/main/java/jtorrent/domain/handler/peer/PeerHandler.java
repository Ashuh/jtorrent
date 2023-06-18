package jtorrent.domain.handler.peer;

import static jtorrent.domain.Constants.PEER_ID;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.model.peer.exception.InfoHashMismatchException;
import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.model.peer.message.KeepAlive;
import jtorrent.domain.model.peer.message.PeerMessage;
import jtorrent.domain.model.peer.message.typed.Bitfield;
import jtorrent.domain.model.peer.message.typed.Cancel;
import jtorrent.domain.model.peer.message.typed.Have;
import jtorrent.domain.model.peer.message.typed.Interested;
import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.peer.message.typed.Request;
import jtorrent.domain.model.peer.message.typed.TypedPeerMessage;
import jtorrent.domain.model.torrent.Block;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.util.Sha1Hash;

public class PeerHandler implements Runnable {

    private static final System.Logger LOGGER = System.getLogger(PeerHandler.class.getName());

    private final Peer peer;
    private final Torrent torrent;
    private final List<Listener> listeners = new ArrayList<>();
    private final Set<Integer> availablePieces = new HashSet<>();

    private boolean isActive = true;
    private boolean isConnected = false;
    private boolean isChoked = true;
    private boolean isBusy = false;

    public PeerHandler(Peer peer, Torrent torrent) {
        this.peer = peer;
        this.torrent = torrent;
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
        peer.sendMessage(request);
    }

    @Override
    public void run() {
        try {
            peer.init();
            handshake();
            sendInterested();
        } catch (IOException e) {
            return;
        }

        while (isActive) {
            try {
                PeerMessage message = peer.receiveMessage();
                handleMessage(message);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Error while communicating with peer {0}", peer);
                isActive = false;
            }
        }
    }

    public void handshake() throws IOException {
        LOGGER.log(Level.DEBUG, "Initiating handshake");
        sendHandshake(torrent.getInfoHash(), PEER_ID.getBytes());
        Handshake inHandshake = peer.receiveHandshake();

        if (!inHandshake.getInfoHash().equals(torrent.getInfoHash())) {
            throw new InfoHashMismatchException(torrent.getInfoHash(), inHandshake.getInfoHash());
        }

        LOGGER.log(Level.DEBUG, "Handshake successful");
        isConnected = true;
    }

    private void sendInterested() throws IOException {
        Interested interested = new Interested();
        peer.sendMessage(interested);
    }

    private void sendHandshake(Sha1Hash infoHash, byte[] peerId) throws IOException {
        Handshake handshake = new Handshake(infoHash, peerId);
        peer.sendMessage(handshake);
    }

    public Set<Integer> getAvailablePieces() {
        return availablePieces;
    }

    private void handleMessage(PeerMessage message) {
        if (message instanceof KeepAlive) {
            handleKeepAlive();
            return;
        }

        assert message instanceof TypedPeerMessage;
        TypedPeerMessage typedMessage = (TypedPeerMessage) message;

        switch (typedMessage.getMessageType()) {
        case CHOKE:
            handleChoke();
            break;
        case UNCHOKE:
            handleUnchoke();
            break;
        case INTERESTED:
            handleInterested();
            break;
        case NOT_INTERESTED:
            handleNotInterested();
            break;
        case HAVE:
            handleHave((Have) typedMessage);
            break;
        case BITFIELD:
            handleBitfield((Bitfield) typedMessage);
            break;
        case REQUEST:
            handleRequest((Request) typedMessage);
            break;
        case PIECE:
            handlePiece((Piece) typedMessage);
            break;
        case CANCEL:
            handleCancel((Cancel) typedMessage);
            break;
        default:
            LOGGER.log(Level.ERROR, "Unknown message type: {0}", typedMessage.getMessageType());
        }
    }

    public void handleKeepAlive() {
        LOGGER.log(Level.DEBUG, "Handling KeepAlive");
    }

    public void handleChoke() {
        LOGGER.log(Level.DEBUG, "Handling Choke");
        listeners.forEach(listener -> listener.onChokeReceived(this));
    }

    public void handleUnchoke() {
        LOGGER.log(Level.DEBUG, "Handling Unchoke");
        isChoked = false;
        listeners.forEach(listener -> listener.onUnchokeRecevied(this));
        notifyIfReady();
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
        listeners.forEach(listener -> listener.onPieceAvailable(this, pieceIndex));
    }

    public void handleBitfield(Bitfield bitfield) {
        LOGGER.log(Level.DEBUG, "Handling Bitfield: {0}", bitfield);
        bitfield.getBits()
                .forEach(i -> {
                    availablePieces.add(i);
                    listeners.forEach(listener -> listener.onPieceAvailable(this, i));
                });
    }

    public void handleRequest(Request request) {
        LOGGER.log(Level.DEBUG, "Handling Request: {0}", request);
    }

    public void handlePiece(Piece piece) {
        LOGGER.log(Level.DEBUG, "Handling piece: {0}", piece);
        isBusy = false;
        listeners.forEach(listener -> listener.onPieceReceived(piece));
        notifyIfReady();
    }

    public void handleCancel(Cancel cancel) {
        LOGGER.log(Level.DEBUG, "Handling Cancel: {0}", cancel);
    }

    private void notifyIfReady() {
        if (!isReady()) {
            return;
        }

        listeners.forEach(listener -> listener.onReady(this));
    }

    public boolean isReady() {
        return isConnected && !isChoked && !isBusy;
    }

    public interface Listener {

        void onUnchokeRecevied(PeerHandler peerHandler);

        void onChokeReceived(PeerHandler peerHandler);

        void onReady(PeerHandler peerHandler);

        void onPieceReceived(Piece piece);

        void onPieceAvailable(PeerHandler peerHandler, int index);
    }
}
