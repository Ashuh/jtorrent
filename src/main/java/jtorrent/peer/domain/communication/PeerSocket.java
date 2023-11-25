package jtorrent.peer.domain.communication;

import static jtorrent.common.domain.Constants.PEER_ID;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.peer.domain.model.exception.InfoHashMismatchException;
import jtorrent.peer.domain.model.exception.UnexpectedEndOfStreamException;
import jtorrent.peer.domain.model.message.Handshake;
import jtorrent.peer.domain.model.message.KeepAlive;
import jtorrent.peer.domain.model.message.PeerMessage;
import jtorrent.peer.domain.model.message.PeerMessageUnpacker;
import jtorrent.peer.domain.model.message.typed.Choke;
import jtorrent.peer.domain.model.message.typed.Interested;
import jtorrent.peer.domain.model.message.typed.Request;
import jtorrent.peer.domain.model.message.typed.Unchoke;

public class PeerSocket {

    private static final Logger LOGGER = System.getLogger(PeerSocket.class.getName());

    private final Socket socket;
    private boolean isConnected;
    private Sha1Hash receivedHandshakeInfoHash;

    public PeerSocket() {
        this(new Socket());
    }

    public PeerSocket(Socket socket) {
        this.socket = requireNonNull(socket);
    }

    private static void checkInfoHashMatch(Sha1Hash expected, Sha1Hash actual) {
        if (!expected.equals(actual)) {
            throw new InfoHashMismatchException(expected, actual);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect(SocketAddress address, Sha1Hash infoHash, boolean isDhtSupported) throws IOException {
        LOGGER.log(Level.INFO, "[{0}] Connecting", address);

        if (isConnected) {
            LOGGER.log(Level.WARNING, "[{0}] Already connected", socket.getRemoteSocketAddress());
            return;
        }

        if (socket.isConnected()) {
            if (!socket.getRemoteSocketAddress().equals(address)) {
                throw new IllegalStateException("Socket is already connected to a different address");
            }
        } else {
            socket.connect(address);
        }

        getReceivedHandshakeInfoHash().ifPresent(receivedInfoHash -> checkInfoHashMatch(receivedInfoHash, infoHash));
        Handshake handshake = new Handshake(infoHash, PEER_ID.getBytes(), isDhtSupported);
        sendMessage(handshake);

        if (!isHandshakeReceived()) {
            Handshake inHandshake = waitForHandshake();
            checkInfoHashMatch(infoHash, inHandshake.getInfoHash());
        }

        isConnected = true;
    }

    public void acceptInboundConnection(boolean isDhtSupported) throws IOException {
        LOGGER.log(Level.INFO, "[{0}] Accepting inbound connection", socket.getRemoteSocketAddress());

        if (isConnected) {
            LOGGER.log(Level.WARNING, "[{0}] Already connected", socket.getRemoteSocketAddress());
            return;
        }

        if (!isHandshakeReceived()) {
            throw new IllegalStateException(
                    String.format("[%s] Connection is not inbound, handshake has not been received",
                            socket.getRemoteSocketAddress()));
        }

        Optional<Sha1Hash> infoHash = getReceivedHandshakeInfoHash();
        assert infoHash.isPresent();

        Handshake outboundHandshake = new Handshake(infoHash.get(), PEER_ID.getBytes(), isDhtSupported);
        sendMessage(outboundHandshake);

        isConnected = true;
    }

    private boolean isHandshakeReceived() {
        return receivedHandshakeInfoHash != null;
    }

    private Optional<Sha1Hash> getReceivedHandshakeInfoHash() {
        return Optional.ofNullable(receivedHandshakeInfoHash);
    }

    public void sendKeepAlive() throws IOException {
        sendMessage(new KeepAlive());
    }

    public void sendChoke() throws IOException {
        sendMessage(new Choke());
    }

    public void sendUnchoke() throws IOException {
        sendMessage(new Unchoke());
    }

    public void sendInterested() throws IOException {
        sendMessage(new Interested());
    }

    public void sendRequest(int index, int begin, int length) throws IOException {
        Request request = new Request(index, begin, length);
        sendMessage(request);
    }

    public void sendMessage(PeerMessage message) throws IOException {
        socket.getOutputStream().write(message.pack());
        LOGGER.log(Level.INFO, "[{0}] Sent: {1}", getPeerContactInfo(), message);
    }

    public PeerContactInfo getPeerContactInfo() {
        if (!socket.isConnected()) {
            throw new IllegalStateException("Socket is not connected");
        }
        return new PeerContactInfo(getRemoteAddress(), getRemotePort());
    }

    public InetAddress getRemoteAddress() {
        return socket.getInetAddress();
    }

    public int getRemotePort() {
        return socket.getPort();
    }

    /**
     * Gets the timeout for socket operations.
     *
     * @throws IOException if an I/O error occurs when getting the timeout
     */
    public int getTimeout() throws IOException {
        return socket.getSoTimeout();
    }

    /**
     * Sets the timeout for socket operations.
     *
     * @param timeout the timeout in milliseconds
     * @throws IOException if an I/O error occurs when setting the timeout
     */
    public void setTimeout(int timeout) throws IOException {
        socket.setSoTimeout(timeout);
    }

    public Handshake waitForHandshake() throws IOException {
        LOGGER.log(Level.DEBUG, "[{0}] Waiting for handshake", getPeerContactInfo());

        byte[] buffer = socket.getInputStream().readNBytes(Handshake.MESSAGE_SIZE_BYTES);
        if (buffer.length != Handshake.MESSAGE_SIZE_BYTES) {
            throw new UnexpectedEndOfStreamException();
        }
        Handshake handshake = Handshake.unpack(buffer);
        receivedHandshakeInfoHash = handshake.getInfoHash();
        LOGGER.log(Level.INFO, "[{0}] Received: {1}", getPeerContactInfo(), handshake);

        return handshake;
    }

    public void close() throws IOException {
        LOGGER.log(Level.DEBUG, "[{0}] Closing PeerSocket", getPeerContactInfo());
        socket.close();
    }

    public PeerMessage receiveMessage() throws IOException {
        LOGGER.log(Level.DEBUG, "Waiting for message");

        int lengthPrefix = readLengthPrefix();

        if (lengthPrefix == 0) {
            return PeerMessageUnpacker.unpack(new byte[0]);
        }

        byte[] messageBytes = readMessageBytes(lengthPrefix);
        PeerMessage peerMessage = PeerMessageUnpacker.unpack(messageBytes);
        LOGGER.log(Level.INFO, "[{0}] Received: {1}", getPeerContactInfo(), peerMessage);
        return peerMessage;
    }

    private int readLengthPrefix() throws IOException {
        byte[] lengthPrefixBytes = socket.getInputStream().readNBytes(Integer.BYTES);

        if (lengthPrefixBytes.length != Integer.BYTES) {
            throw new UnexpectedEndOfStreamException();
        }

        return ByteBuffer.wrap(lengthPrefixBytes).getInt();
    }

    private byte[] readMessageBytes(int lengthPrefix) throws IOException {
        byte[] messageBytes = socket.getInputStream().readNBytes(lengthPrefix);

        if (messageBytes.length != lengthPrefix) {
            throw new UnexpectedEndOfStreamException();
        }

        return messageBytes;
    }
}
