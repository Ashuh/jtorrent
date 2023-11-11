package jtorrent.dht.domain.model.message;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TransactionId {

    private final char value;

    public TransactionId(char value) {
        this.value = value;
    }

    /**
     * Generates a random transaction id.
     */
    public static TransactionId generateRandom() {
        char value = (char) (Math.random() * Character.MAX_VALUE);
        return new TransactionId(value);
    }

    /**
     * Creates a {@link TransactionId} from the given bytes.
     *
     * @param bytes The bytes to create the transaction id from.
     * @return The transaction id.
     * @throws IllegalArgumentException If the given byte array is not exactly 2 bytes long.
     */
    public static TransactionId fromBytes(byte[] bytes) {
        if (bytes.length != Character.BYTES) {
            throw new IllegalArgumentException(
                    String.format("Transaction id must be %d bytes long, but was %d bytes long",
                            Character.BYTES, bytes.length));
        }
        char value = (char) ((Byte.toUnsignedInt(bytes[0]) << 8) | (Byte.toUnsignedInt(bytes[1])));
        return new TransactionId(value);
    }

    public char getValue() {
        return value;
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((value >> 8) & 0xFF);
        bytes[1] = (byte) (value & 0xFF);
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionId that = (TransactionId) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((value >> 8) & 0xFF);
        bytes[1] = (byte) (value & 0xFF);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
