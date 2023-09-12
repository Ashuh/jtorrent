package jtorrent.domain.util;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Represents an immutable byte array of a fixed length.
 */
public abstract class ByteArray {

    protected final byte[] bytes;

    /**
     * Creates a new {@link ByteArray} of the given length.
     *
     * @param length the length of the byte array
     */
    protected ByteArray(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length must be positive");
        }

        this.bytes = new byte[length];
    }

    /**
     * Creates a new {@link ByteArray} from the given byte array by copying it.
     *
     * @param bytes the byte array to copy
     */
    protected ByteArray(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes cannot be null");
        }

        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Returns a copy of the underlying byte array.
     */
    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Returns the length of the underlying byte array.
     */
    public int getLength() {
        return bytes.length;
    }

    /**
     * Converts this byte array into a {@link BigInteger}.
     * The value is interpreted as an unsigned integer.
     *
     * @return the {@link BigInteger} representation of this byte array
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ByteArray byteArray = (ByteArray) o;
        return Arrays.equals(bytes, byteArray.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
