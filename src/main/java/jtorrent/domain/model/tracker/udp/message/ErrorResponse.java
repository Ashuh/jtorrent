package jtorrent.domain.model.tracker.udp.message;

import java.nio.ByteBuffer;

/**
 * Represents an error response.
 *
 * @see <a href="https://www.bittorrent.org/beps/bep_0015.html">UDP Tracker Protocol for BitTorrent</a>
 */
public class ErrorResponse {

    private final int transactionId;
    private final String message;

    public ErrorResponse(int transactionId, String message) {
        this.transactionId = transactionId;
        this.message = message;
    }

    public static ErrorResponse unpack(byte[] payload) {
        if (payload.length < Integer.BYTES) {
            throw new IllegalArgumentException();
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int transactionId = buffer.getInt();
        byte[] messageBytes = new byte[buffer.limit() - Integer.BYTES];
        buffer.get(messageBytes);
        String message = new String(messageBytes);
        return new ErrorResponse(transactionId, message);
    }

    public int getTransactionId() {
        return transactionId;
    }

    public String getMessage() {
        return message;
    }
}
