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
import java.util.Objects;

import jtorrent.domain.model.peer.exception.IncompleteReadException;
import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.model.peer.message.KeepAlive;
import jtorrent.domain.model.peer.message.PeerMessage;
import jtorrent.domain.model.peer.message.typed.Bitfield;
import jtorrent.domain.model.peer.message.typed.Cancel;
import jtorrent.domain.model.peer.message.typed.Choke;
import jtorrent.domain.model.peer.message.typed.Have;
import jtorrent.domain.model.peer.message.typed.Interested;
import jtorrent.domain.model.peer.message.typed.MessageType;
import jtorrent.domain.model.peer.message.typed.NotInterested;
import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.peer.message.typed.Request;
import jtorrent.domain.model.peer.message.typed.Unchoke;

public class Peer {

    private static final Logger LOGGER = System.getLogger(Peer.class.getName());
    private final InetAddress address;
    private final int port; // unsigned short

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    public Peer(InetAddress address, int port) {
        this.address = requireNonNull(address);
        this.port = port;
    }

    public void init() throws IOException {
        LOGGER.log(Level.DEBUG, "Connecting to peer: {0}:{1}", address, port);
        this.socket = new Socket(address, port);
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        LOGGER.log(Level.DEBUG, "Connected to peer: {0}:{1}", address, port);
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

    public PeerMessage receiveMessage() throws IOException {
        LOGGER.log(Level.DEBUG, "Waiting for message");
        byte[] lengthPrefix = inputStream.readNBytes(Integer.BYTES);

        if (lengthPrefix.length != Integer.BYTES) {
            throw new IncompleteReadException(Integer.BYTES, lengthPrefix.length);
        }

        int length = ByteBuffer.wrap(lengthPrefix).getInt();

        if (length == 0) {
            LOGGER.log(Level.DEBUG, "Received KeepAlive message");
            return new KeepAlive();
        }

        byte id = (byte) inputStream.read();
        byte[] payload = inputStream.readNBytes(length - 1);

        if (payload.length != length - 1) {
            throw new IncompleteReadException(length - 1, payload.length);
        }

        return unpackMessagePayload(id, payload);
    }

    private PeerMessage unpackMessagePayload(byte id, byte[] payload) {
        MessageType messageType = MessageType.fromValue(id);
        LOGGER.log(Level.DEBUG, "Received message of type: {0}", messageType);

        switch (messageType) {
        case CHOKE:
            return new Choke();
        case UNCHOKE:
            return new Unchoke();
        case INTERESTED:
            return new Interested();
        case NOT_INTERESTED:
            return new NotInterested();
        case HAVE:
            return Have.unpack(payload);
        case BITFIELD:
            return Bitfield.unpack(payload);
        case REQUEST:
            return Request.unpack(payload);
        case PIECE:
            return Piece.unpack(payload);
        case CANCEL:
            return Cancel.unpack(payload);
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
        return "Peer{"
                + "address=" + address
                + ", port=" + port
                + '}';
    }
}
