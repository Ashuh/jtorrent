package jtorrent.domain.tracker.model.udp.message.response;

import java.nio.ByteBuffer;

import jtorrent.domain.common.exception.UnpackException;

/**
 * Represents an error response.
 *
 * @see <a href="https://www.bittorrent.org/beps/bep_0015.html">UDP Tracker Protocol for BitTorrent</a>
 */
public class UdpErrorResponse {

    private final int transactionId;
    private final String message;

    public UdpErrorResponse(int transactionId, String message) {
        this.transactionId = transactionId;
        this.message = message;
    }

    public static UdpErrorResponse unpack(byte[] payload) {
        if (payload.length < Integer.BYTES) {
            throw new UnpackException(
                    "Expected at least " + Integer.BYTES + " bytes but got " + payload.length + " bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int transactionId = buffer.getInt();
        byte[] messageBytes = new byte[buffer.limit() - Integer.BYTES];
        buffer.get(messageBytes);
        String message = new String(messageBytes);
        return new UdpErrorResponse(transactionId, message);
    }

    public int getTransactionId() {
        return transactionId;
    }

    public String getMessage() {
        return message;
    }
}
