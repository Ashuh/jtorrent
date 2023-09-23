package jtorrent.domain.model.peer;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import jtorrent.domain.model.peer.exception.UnexpectedEndOfStreamException;
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
import jtorrent.domain.model.peer.message.typed.Port;
import jtorrent.domain.model.peer.message.typed.Request;
import jtorrent.domain.model.peer.message.typed.Unchoke;
import jtorrent.domain.util.DurationWindow;
import jtorrent.domain.util.Sha1Hash;

public abstract class Peer {

    private static final Logger LOGGER = System.getLogger(Peer.class.getName());

    protected final InetAddress address;
    protected final int port; // unsigned short
    protected Socket socket;
    protected final DurationWindow durationWindow = new DurationWindow(Duration.ofSeconds(20));
    protected boolean isLocalChoked = true;
    protected boolean isRemoteChoked = true;

    protected Peer(InetAddress address, int port) {
        this.address = requireNonNull(address);
        this.port = port;
    }

    protected Peer(Socket socket) {
        this.socket = requireNonNull(socket);
        this.address = socket.getInetAddress();
        this.port = socket.getPort();
    }

    public byte[] toCompactPeerInfo() {
        return ByteBuffer.allocate(6)
                .put(address.getAddress())
                .putShort((short) port)
                .array();
    }

    public abstract void connect(Sha1Hash infoHash, boolean isDhtSupported) throws IOException;

    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
        }
        durationWindow.close();
    }

    public void sendMessage(PeerMessage message) throws IOException {
        LOGGER.log(Level.DEBUG, "Sending message: {0}", message);
        socket.getOutputStream().write(message.pack());
    }

    public Handshake receiveHandshake() throws IOException {
        LOGGER.log(Level.DEBUG, "Waiting for handshake");
        byte[] buffer = socket.getInputStream().readNBytes(Handshake.BYTES);
        if (buffer.length != Handshake.BYTES) {
            throw new UnexpectedEndOfStreamException();
        }
        LOGGER.log(Level.DEBUG, "Received handshake");
        return Handshake.unpack(buffer);
    }

    public PeerMessage receiveMessage() throws IOException {
        LOGGER.log(Level.DEBUG, "Waiting for message");
        byte[] lengthPrefix = socket.getInputStream().readNBytes(Integer.BYTES);

        if (lengthPrefix.length != Integer.BYTES) {
            throw new UnexpectedEndOfStreamException();
        }

        int length = ByteBuffer.wrap(lengthPrefix).getInt();

        if (length == 0) {
            LOGGER.log(Level.DEBUG, "Received KeepAlive message");
            return new KeepAlive();
        }

        byte id = (byte) socket.getInputStream().read();
        if (id == -1) {
            throw new UnexpectedEndOfStreamException();
        }

        byte[] payload = socket.getInputStream().readNBytes(length - 1);

        if (payload.length != length - 1) {
            throw new UnexpectedEndOfStreamException();
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
            Piece piece = Piece.unpack(payload);
            durationWindow.add(piece.getBlock().length);
            return piece;
        case CANCEL:
            return Cancel.unpack(payload);
        case PORT:
            return Port.unpack(payload);
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

    public boolean isLocalChoked() {
        return isLocalChoked;
    }

    public void setLocalChoked(boolean localChoked) {
        isLocalChoked = localChoked;
    }

    public boolean isRemoteChoked() {
        return isRemoteChoked;
    }

    public void setRemoteChoked(boolean remoteChoked) {
        isRemoteChoked = remoteChoked;
    }

    public double getDownloadRate() {
        return durationWindow.getRate();
    }

    public Observable<Double> getDownloadRateObservable() {
        return durationWindow.getRateObservable();
    }

    public double getUploadRate() {
        return 0; // TODO: implement
    }

    public Observable<Double> getUploadRateObservable() {
        return Observable.never(); // TODO: implement
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
