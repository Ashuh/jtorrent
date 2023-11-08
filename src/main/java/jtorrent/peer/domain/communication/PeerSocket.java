package jtorrent.peer.domain.communication;

import static jtorrent.common.domain.Constants.PEER_ID;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.peer.domain.model.exception.InfoHashMismatchException;
import jtorrent.peer.domain.model.exception.UnexpectedEndOfStreamException;
import jtorrent.peer.domain.model.message.Handshake;
import jtorrent.peer.domain.model.message.PeerMessage;
import jtorrent.peer.domain.model.message.PeerMessageUnpacker;
import jtorrent.common.domain.util.Sha1Hash;

public class PeerSocket {

    private static final Logger LOGGER = System.getLogger(PeerSocket.class.getName());

    private final PeerMessageUnpacker peerMessageUnpacker = new PeerMessageUnpacker();
    private final Socket socket;
    private boolean isConnected;
    private boolean isHandshakeReceived;

    public PeerSocket(PeerContactInfo peerContactInfo) throws IOException {
        this(new Socket(peerContactInfo.getAddress(), peerContactInfo.getPort()));
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

    public void connect(Sha1Hash infoHash, boolean isDhtSupported) throws IOException {
        if (isConnected) {
            return;
        }

        if (!socket.isConnected()) {
            throw new IllegalStateException("Socket is not connected");
        }

        requireNonNull(infoHash);
        LOGGER.log(Level.DEBUG, "Connecting to {0}", socket.getRemoteSocketAddress());
        Handshake handshake = new Handshake(infoHash, PEER_ID.getBytes(), isDhtSupported);
        sendMessage(handshake);

        if (!isHandshakeReceived) {
            Handshake inHandshake = waitForHandshake();
            if (!inHandshake.getInfoHash().equals(infoHash)) {
                throw new InfoHashMismatchException(infoHash, inHandshake.getInfoHash());
            }
        }

        isConnected = true;
        LOGGER.log(Level.DEBUG, "Sent handshake {0}", socket.getRemoteSocketAddress());
    }

    public void sendMessage(PeerMessage message) throws IOException {
        LOGGER.log(Level.DEBUG, "Sending message: {0}", message);
        socket.getOutputStream().write(message.pack());
    }

    public Handshake waitForHandshake() throws IOException {
        LOGGER.log(Level.DEBUG, "Waiting for handshake");
        byte[] buffer = socket.getInputStream().readNBytes(Handshake.BYTES);
        if (buffer.length != Handshake.BYTES) {
            throw new UnexpectedEndOfStreamException();
        }
        Handshake handshake = Handshake.unpack(buffer);
        isHandshakeReceived = true;
        LOGGER.log(Level.DEBUG, "Received handshake " + handshake);
        return handshake;
    }

    public void close() throws IOException {
        LOGGER.log(Level.DEBUG, "Closing PeerSocket {0}", socket.getRemoteSocketAddress());
        socket.close();
    }

    public PeerMessage receiveMessage() throws IOException {
        LOGGER.log(Level.DEBUG, "Waiting for message");

        int lengthPrefix = readLengthPrefix();

        if (lengthPrefix == 0) {
            return peerMessageUnpacker.unpack(new byte[0]);
        }

        byte[] messageBytes = readMessageBytes(lengthPrefix);
        return peerMessageUnpacker.unpack(messageBytes);
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
