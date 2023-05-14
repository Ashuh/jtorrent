package jtorrent.domain.model.tracker.udp.message;

import java.nio.ByteBuffer;
import java.util.Objects;

import jtorrent.domain.model.exception.UnpackException;

/**
 * Represents a connection response.
 *
 * @see <a href="https://www.bittorrent.org/beps/bep_0015.html">UDP Tracker Protocol for BitTorrent</a>
 */
public class ConnectionResponse extends UdpMessage {

    public static final int MESSAGE_BYTES = 16;
    public static final int PAYLOAD_BYTES = 12;

    private final long connectionId;

    public ConnectionResponse(int transactionId, long connectionId) {
        super(transactionId);
        this.connectionId = connectionId;
    }

    public static ConnectionResponse unpack(byte[] payload) {
        if (payload.length != PAYLOAD_BYTES) {
            throw new UnpackException("Expected " + PAYLOAD_BYTES + " bytes but got " + payload.length + " bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int transactionId = buffer.getInt();
        long connectionId = buffer.getLong();
        return new ConnectionResponse(transactionId, connectionId);
    }

    public long getConnectionId() {
        return connectionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionResponse that = (ConnectionResponse) o;
        return connectionId == that.connectionId;
    }

    @Override
    public String toString() {
        return "ConnectionResponse{"
                + "connectionId=" + connectionId
                + ", transactionId=" + transactionId
                + '}';
    }
}
