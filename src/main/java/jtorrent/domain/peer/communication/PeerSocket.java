package jtorrent.domain.peer.communication;

import static jtorrent.domain.common.Constants.PEER_ID;
import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.peer.model.exception.InfoHashMismatchException;
import jtorrent.domain.peer.model.exception.UnexpectedEndOfStreamException;
import jtorrent.domain.peer.model.message.Handshake;
import jtorrent.domain.peer.model.message.PeerMessage;
import jtorrent.domain.peer.model.message.factory.PeerMessageUnpacker;

public class PeerSocket {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerSocket.class);

    private final Socket socket;
    private boolean isConnected;
    private Handshake receivedHandshake;

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
        if (isConnected) {
            LOGGER.debug(Markers.PEER, "Already connected");
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
        if (isConnected) {
            LOGGER.debug(Markers.PEER, "Already connected");
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
        return receivedHandshake != null;
    }

    private Optional<Sha1Hash> getReceivedHandshakeInfoHash() {
        return Optional.ofNullable(receivedHandshake).map(Handshake::getInfoHash);
    }

    public boolean isDhtSupportedByRemote() {
        if (!isHandshakeReceived()) {
            throw new IllegalStateException("Handshake has not been received");
        }

        return receivedHandshake.isDhtSupported();
    }

    public void sendMessage(PeerMessage message) throws IOException {
        socket.getOutputStream().write(message.pack());
        LOGGER.debug(Markers.PEER, "Sent: {}", message);
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
        LOGGER.debug(Markers.PEER, "Waiting for handshake");

        byte[] buffer = socket.getInputStream().readNBytes(Handshake.MESSAGE_SIZE_BYTES);
        if (buffer.length != Handshake.MESSAGE_SIZE_BYTES) {
            throw new UnexpectedEndOfStreamException();
        }
        Handshake handshake = Handshake.unpack(buffer);
        receivedHandshake = handshake;
        LOGGER.debug(Markers.PEER, "Received handshake: {}", handshake);

        return handshake;
    }

    public void close() throws IOException {
        LOGGER.debug(Markers.PEER, "Closing PeerSocket");
        socket.close();
    }

    public PeerMessage receiveMessage() throws IOException {
        LOGGER.debug(Markers.PEER, "Waiting for message");

        int lengthPrefix = readLengthPrefix();

        if (lengthPrefix == 0) {
            return PeerMessageUnpacker.unpack(new byte[0]);
        }

        byte[] messageBytes = readMessageBytes(lengthPrefix);
        PeerMessage peerMessage = PeerMessageUnpacker.unpack(messageBytes);
        LOGGER.debug(Markers.PEER, "Received: {}", peerMessage);
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
