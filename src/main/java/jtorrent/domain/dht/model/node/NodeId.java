package jtorrent.domain.dht.model.node;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import jtorrent.domain.common.util.Bit160Value;

public class NodeId extends Bit160Value {

    public static final NodeId LOCAL;
    public static final NodeId ZERO = NodeId.fromBigInteger(BigInteger.ZERO);
    public static final NodeId MAX =
            NodeId.fromBigInteger(BigInteger.TWO.pow(Bit160Value.SIZE_BITS).subtract(BigInteger.ONE));
    private static final Random RANDOM = new Random();

    static {
        LOCAL = NodeId.random();
    }

    public NodeId(byte[] id) {
        super(id);
    }

    public static NodeId random() {
        byte[] id = new byte[Bit160Value.SIZE_BYTES];
        RANDOM.nextBytes(id);
        return new NodeId(id);
    }

    // TODO: quick solution to get a random node id with a given prefix. Improve this.
    public static NodeId randomWithPrefix(NodeIdPrefix prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }

        byte[] id = new byte[Bit160Value.SIZE_BYTES];
        RANDOM.nextBytes(id);

        for (int i = 0; i < prefix.getLength(); i++) {
            id[i] |= prefix.getBytes()[i];
        }

        return new NodeId(id);
    }

    public static NodeId fromBigInteger(BigInteger id) {
        if (id == null) {
            throw new IllegalArgumentException("Node ID cannot be null");
        }

        if (id.bitLength() > Bit160Value.SIZE_BITS) {
            throw new IllegalArgumentException("Node ID cannot be longer than 160 bits");
        }

        if (id.signum() == -1) {
            throw new IllegalArgumentException("Node ID cannot be negative");
        }

        byte[] bytes = id.toByteArray();

        if (bytes.length > Bit160Value.SIZE_BYTES) {
            // Remove extra leading byte used for the sign bit
            // Only required if the magnitude occupies all 160 bits, in which case the byte array will be 21 bytes long
            // If the magnitude is less than 160 bits, the byte array will be shorter than 21 bytes, and the sign bit
            // will be 0, so the extra leading byte will be 0 and can be ignored
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        } else if (bytes.length < Bit160Value.SIZE_BYTES) {
            // Pad with leading zeros
            byte[] padded = new byte[Bit160Value.SIZE_BYTES];
            System.arraycopy(bytes, 0, padded, Bit160Value.SIZE_BYTES - bytes.length, bytes.length);
            bytes = padded;
        }

        return new NodeId(bytes);
    }

    public BigInteger distanceTo(Bit160Value other) {
        return xor(other).toBigInteger();
    }

    /**
     * Creates a {@link NodeIdPrefix} from this {@link NodeId} and bitLength.
     *
     * @param bitLength The number of bits to include in the prefix. Must be between 0 and 160 (inclusive).
     * @return the {@link NodeIdPrefix} of this {@link NodeId} with the given bitLength.
     */
    public NodeIdPrefix getPrefix(int bitLength) {
        if (bitLength < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }

        if (bitLength > Bit160Value.SIZE_BITS) {
            throw new IllegalArgumentException("Length cannot be greater than " + Bit160Value.SIZE_BITS + " bits");
        }

        int numFullBytes = bitLength / Byte.SIZE;
        int numTrailingBits = bitLength % Byte.SIZE;
        int numBytesToCopy = numFullBytes + (numTrailingBits > 0 ? 1 : 0);
        byte[] bytes = Arrays.copyOf(getBytes(), numBytesToCopy);
        // Clear the trailing bits
        if (numTrailingBits > 0) {
            bytes[numFullBytes] &= (byte) (0xFF << (Byte.SIZE - numTrailingBits));
        }
        return new NodeIdPrefix(bytes, bitLength);
    }

    @Override
    public String toString() {
        return toBigInteger().toString(2);
    }
}
