package jtorrent.domain.model.peer;

import static jtorrent.domain.Constants.PEER_ID;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jtorrent.domain.model.peer.exception.InfoHashMismatchException;
import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.model.peer.message.typed.Bitfield;
import jtorrent.domain.model.peer.message.typed.Cancel;
import jtorrent.domain.model.peer.message.typed.Have;
import jtorrent.domain.model.peer.message.typed.Interested;
import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.peer.message.typed.Request;
import jtorrent.domain.model.torrent.Block;
import jtorrent.domain.model.torrent.Sha1Hash;
import jtorrent.domain.model.torrent.Torrent;

public class PeerHandler implements Runnable, Peer.Listener {

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
        this.peer.addListener(this);
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void assignBlock(Block block) throws IOException {
        LOGGER.log(Level.DEBUG, "Assign Block: {0}", block);

        int index = block.getIndex();
        int offset = block.getOffset();
        int length = block.getLength();
        sendRequest(index, offset, length);
    }

    private void sendRequest(int index, int begin, int length) throws IOException {
        Request request = new Request(index, begin, length);
        peer.sendMessage(request);
        isBusy = true;
    }

    @Override
    public void run() {
        try {
            peer.init();
            handshake();
            sendInterested();

            while (isActive) {
                peer.receiveMessage();
            }
        } catch (IOException e) {
            // TODO: handle exceptions
            e.printStackTrace();
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

    @Override
    public void onKeepAlive() {
        LOGGER.log(Level.DEBUG, "Handling KeepAlive");
    }

    @Override
    public void onChoke() {
        LOGGER.log(Level.DEBUG, "Handling Choke");
    }

    @Override
    public void onUnchoke() {
        LOGGER.log(Level.DEBUG, "Handling Unchoke");
        isChoked = false;
        notifyIfReady();
    }

    @Override
    public void onInterested() {
        LOGGER.log(Level.DEBUG, "Handling Interested");
    }

    @Override
    public void onNotInterested() {
        LOGGER.log(Level.DEBUG, "Handling NotInterested");
    }

    @Override
    public void onHave(Have have) {
        LOGGER.log(Level.DEBUG, "Handling Have: {0}", have);
        int pieceIndex = have.getPieceIndex();
        availablePieces.add(pieceIndex);
        listeners.forEach(listener -> listener.onPieceAvailable(this, pieceIndex));
    }

    @Override
    public void onBitfield(Bitfield bitfield) {
        LOGGER.log(Level.DEBUG, "Handling Bitfield: {0}", bitfield);
    }

    @Override
    public void onRequest(Request request) {
        LOGGER.log(Level.DEBUG, "Handling Request: {0}", request);
    }

    @Override
    public void onPiece(Piece piece) {
        LOGGER.log(Level.DEBUG, "Handling piece: {0}", piece);
        isBusy = false;
        listeners.forEach(listener -> listener.onPieceReceived(piece));
        notifyIfReady();
    }

    @Override
    public void onCancel(Cancel cancel) {
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

        void onReady(PeerHandler peerHandler);

        void onPieceReceived(Piece piece);

        void onPieceAvailable(PeerHandler peerHandler, int index);
    }
}
