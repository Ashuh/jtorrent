package jtorrent.common.domain.util;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Bit160Value extends ByteArray {

    public static final int SIZE_BITS = 160;
    public static final int SIZE_BYTES = SIZE_BITS / 8;
    public static final Bit160Value MAX;

    static {
        byte[] maxBytes = new byte[SIZE_BYTES];
        Arrays.fill(maxBytes, (byte) 0xFF);
        MAX = new Bit160Value(maxBytes);
    }

    public Bit160Value(byte[] bytes) {
        super(validateBytes(bytes));
    }

    /**
     * Checks that the byte array is not null and is 20 bytes long.
     * Returns the byte array if it is valid or throws an exception if it is not.
     *
     * @param bytes the byte array to validate
     * @return the byte array if it is valid
     * @throws IllegalArgumentException if the byte array is null or not 20 bytes long
     */
    private static byte[] validateBytes(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes cannot be null");
        }

        if (bytes.length != SIZE_BYTES) {
            throw new IllegalArgumentException("Byte array must be 20 bytes long");
        }

        return bytes;
    }

    public Bit160Value xor(Bit160Value other) {
        if (other == null) {
            throw new IllegalArgumentException("Other prefix cannot be null");
        }

        byte[] result = new byte[SIZE_BYTES];
        for (int i = 0; i < SIZE_BYTES; i++) {
            result[i] = (byte) (bytes[i] ^ other.bytes[i]);
        }

        return new Bit160Value(result);
    }

    public int numMatchingBits(Bit160Value value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        byte[] thisBytes = getBytes();
        byte[] otherBytes = value.getBytes();

        return IntStream.range(0, SIZE_BYTES)
                .map(i -> numMatchingBits(thisBytes[i], otherBytes[i]))
                .sum();
    }

    private static int numMatchingBits(byte b1, byte b2) {
        int xor = b1 ^ b2;
        int matchingBits = 0;
        if (xor == 0) {
            matchingBits += Byte.SIZE;
        } else {
            for (int j = 0; j < Byte.SIZE; j++) {
                int mask = 1 << (Byte.SIZE - j - 1);
                if ((xor & mask) == 0) {
                    matchingBits++;
                } else {
                    break;
                }
            }
        }
        return matchingBits;
    }
}
