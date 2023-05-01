package jtorrent.domain.model.tracker.udp.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents a connection request.
 *
 * @see <a href="https://www.bittorrent.org/beps/bep_0015.html">UDP Tracker Protocol for BitTorrent</a>
 */
public class ConnectionRequest extends Request {

    public static final int BYTES = 16;
    private static final long PROTOCOL_ID = 0x41727101980L; // magic constant

    public int getTransactionId() {
        return transactionId;
    }

    @Override
    public byte[] pack() {
        return ByteBuffer.allocate(BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(PROTOCOL_ID)
                .putInt(Action.CONNECT.getValue())
                .putInt(transactionId)
                .array();
    }

    @Override
    public String toString() {
        return "ConnectionRequest{" +
                "transactionId=" + transactionId +
                '}';
    }
}
