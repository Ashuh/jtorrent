package jtorrent.domain.model.peer;

import static jtorrent.domain.Constants.PEER_ID;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import jtorrent.domain.model.peer.exception.InfoHashMismatchException;
import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.util.Sha1Hash;

public class OutgoingPeer extends Peer {

    private static final System.Logger LOGGER = System.getLogger(OutgoingPeer.class.getName());
    private static final int COMPACT_PEER_INFO_BYTES = 6;

    public OutgoingPeer(InetAddress address, int port) {
        super(address, port);
    }

    public static Peer fromCompactPeerInfo(byte[] bytes) {
        if (bytes.length != COMPACT_PEER_INFO_BYTES) {
            throw new IllegalArgumentException(
                    String.format("Compact peer info must be %d bytes long", COMPACT_PEER_INFO_BYTES));
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        byte[] addressBytes = new byte[Integer.BYTES];
        buffer.get(addressBytes);
        InetAddress address;
        try {
            address = InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }

        int port = Short.toUnsignedInt(buffer.getShort());
        return new OutgoingPeer(address, port);
    }

    @Override
    public void connect(Sha1Hash infoHash, boolean isDhtSupported) throws IOException {
        if (infoHash == null) {
            throw new IllegalArgumentException("infoHash cannot be null");
        }

        LOGGER.log(Level.DEBUG, "Connecting to peer: {0}:{1}", address, port);
        this.socket = new Socket(address, port);

        Handshake handshake = new Handshake(infoHash, PEER_ID.getBytes(), isDhtSupported);
        sendMessage(handshake);

        Handshake inHandshake = receiveHandshake();
        if (!inHandshake.getInfoHash().equals(infoHash)) {
            throw new InfoHashMismatchException(infoHash, inHandshake.getInfoHash());
        }

        LOGGER.log(System.Logger.Level.DEBUG, "Sent handshake to peer: {0}:{1}", address, port);
    }
}
