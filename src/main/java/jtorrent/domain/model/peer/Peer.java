package jtorrent.domain.model.peer;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jtorrent.domain.model.peer.exception.IncompleteReadException;
import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.model.peer.message.PeerMessage;
import jtorrent.domain.model.peer.message.typed.Bitfield;
import jtorrent.domain.model.peer.message.typed.Cancel;
import jtorrent.domain.model.peer.message.typed.Have;
import jtorrent.domain.model.peer.message.typed.MessageType;
import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.peer.message.typed.Request;

public class Peer {

    private static final Logger LOGGER = System.getLogger(Peer.class.getName());
    private final InetAddress address;
    private final int port; // unsigned short
    private final List<Listener> listeners = new ArrayList<>();

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    public Peer(InetAddress address, int port) {
        this.address = requireNonNull(address);
        this.port = port;
    }

    public void init() throws IOException {
        this.socket = new Socket(address, port);
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void sendMessage(PeerMessage message) throws IOException {
        LOGGER.log(Level.DEBUG, "Sending message: {0}", message);
        outputStream.write(message.pack());
    }

    public Handshake receiveHandshake() throws IOException {
        LOGGER.log(Level.DEBUG, "Waiting for handshake");
        byte[] buffer = inputStream.readNBytes(Handshake.BYTES);
        if (buffer.length != Handshake.BYTES) {
            throw new IncompleteReadException(Handshake.BYTES, buffer.length);
        }
        LOGGER.log(Level.DEBUG, "Received handshake");
        return Handshake.unpack(buffer);
    }

    public void receiveMessage() throws IOException {
        LOGGER.log(Level.DEBUG, "Waiting for message");
        byte[] lengthPrefix = inputStream.readNBytes(Integer.BYTES);

        if (lengthPrefix.length != Integer.BYTES) {
            throw new IncompleteReadException(Integer.BYTES, lengthPrefix.length);
        }

        int length = ByteBuffer.wrap(lengthPrefix).getInt();

        if (length == 0) {
            LOGGER.log(Level.DEBUG, "Received KeepAlive message");
            listeners.forEach(Listener::onKeepAlive);
            return;
        }

        byte id = (byte) inputStream.read();
        byte[] payload = inputStream.readNBytes(length - 1);

        if (payload.length != length - 1) {
            throw new IncompleteReadException(length - 1, payload.length);
        }

        handleTypedMessage(id, payload);
    }

    private void handleTypedMessage(byte id, byte[] payload) {
        MessageType messageType = MessageType.fromValue(id);
        LOGGER.log(Level.DEBUG, "Received message of type: {0}", messageType);

        switch (messageType) {
        case CHOKE:
            listeners.forEach(Listener::onChoke);
            break;
        case UNCHOKE:
            listeners.forEach(Listener::onUnchoke);
            break;
        case INTERESTED:
            listeners.forEach(Listener::onInterested);
            break;
        case NOT_INTERESTED:
            listeners.forEach(Listener::onNotInterested);
            break;
        case HAVE:
            Have have = Have.unpack(payload);
            listeners.forEach(listener -> listener.onHave(have));
            break;
        case BITFIELD:
            Bitfield bitfield = Bitfield.unpack(payload);
            listeners.forEach(listener -> listener.onBitfield(bitfield));
            break;
        case REQUEST:
            Request request = Request.unpack(payload);
            listeners.forEach(listener -> listener.onRequest(request));
            break;
        case PIECE:
            Piece piece = Piece.unpack(payload);
            listeners.forEach(listener -> listener.onPiece(piece));
            break;
        case CANCEL:
            Cancel cancel = Cancel.unpack(payload);
            listeners.forEach(listener -> listener.onCancel(cancel));
            break;
        default:
            throw new IllegalArgumentException("Unsupported message type: " + messageType);
        }
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Peer peer = (Peer) o;
        return port == peer.port && address.equals(peer.address);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "address=" + address +
                ", port=" + port +
                '}';
    }

    public interface Listener {

        void onKeepAlive();

        void onChoke();

        void onUnchoke();

        void onInterested();

        void onNotInterested();

        void onHave(Have have);

        void onBitfield(Bitfield bitfield);

        void onRequest(Request request);

        void onPiece(Piece piece);

        void onCancel(Cancel cancel);
    }
}
