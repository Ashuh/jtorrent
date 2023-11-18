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

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.peer.domain.model.exception.InfoHashMismatchException;
import jtorrent.peer.domain.model.exception.UnexpectedEndOfStreamException;
import jtorrent.peer.domain.model.message.Handshake;
import jtorrent.peer.domain.model.message.PeerMessage;
import jtorrent.peer.domain.model.message.PeerMessageUnpacker;

public class PeerSocket {

    private static final Logger LOGGER = System.getLogger(PeerSocket.class.getName());

    private final PeerMessageUnpacker peerMessageUnpacker = new PeerMessageUnpacker();
    private final Socket socket;
    private boolean isConnected;
    private boolean isHandshakeReceived;

    public PeerSocket() {
        this(new Socket());
    }

    public PeerSocket(Socket socket) {
        this.socket = requireNonNull(socket);
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

    public boolean isConnected() {
        return isConnected;
    }

    public void connect(SocketAddress address, Sha1Hash infoHash, boolean isDhtSupported) throws IOException {
        LOGGER.log(Level.INFO, "[{0}] Connecting", socket.getRemoteSocketAddress());

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

        Handshake handshake = new Handshake(infoHash, PEER_ID.getBytes(), isDhtSupported);
        sendMessage(handshake);

        if (!isHandshakeReceived) {
            Handshake inHandshake = waitForHandshake();
            if (!inHandshake.getInfoHash().equals(infoHash)) {
                throw new InfoHashMismatchException(infoHash, inHandshake.getInfoHash());
            }
        }

        isConnected = true;
    }

    public void sendMessage(PeerMessage message) throws IOException {
        socket.getOutputStream().write(message.pack());
        LOGGER.log(Level.INFO, "[{0}] Sent: {1}", getPeerContactInfo(), message);

    }

    public Handshake waitForHandshake() throws IOException {
        LOGGER.log(Level.DEBUG, "[{0}] Waiting for handshake", getPeerContactInfo());

        byte[] buffer = socket.getInputStream().readNBytes(Handshake.MESSAGE_SIZE_BYTES);
        if (buffer.length != Handshake.MESSAGE_SIZE_BYTES) {
            throw new UnexpectedEndOfStreamException();
        }
        Handshake handshake = Handshake.unpack(buffer);
        isHandshakeReceived = true;
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
            return peerMessageUnpacker.unpack(new byte[0]);
        }

        byte[] messageBytes = readMessageBytes(lengthPrefix);
        PeerMessage peerMessage = peerMessageUnpacker.unpack(messageBytes);
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
