package jtorrent.domain.model.dht.node;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class NodeId implements Comparable<NodeId> {

    public static final int SIZE = 160;
    public static final int BYTES = SIZE / 8;
    public static final NodeId ZERO = NodeId.fromBigInteger(BigInteger.ZERO);
    public static final NodeId MAX = NodeId.fromBigInteger(BigInteger.TWO.pow(SIZE).subtract(BigInteger.ONE));
    private static final Random RANDOM = new Random();

    private final byte[] id;

    public NodeId(byte[] id) {
        if (id == null) {
            throw new IllegalArgumentException("Node ID cannot be null");
        }

        if (id.length != BYTES) {
            throw new IllegalArgumentException("Node ID must be 20 bytes long");
        }

        this.id = id;
    }

    public static NodeId random() {
        byte[] id = new byte[BYTES];
        RANDOM.nextBytes(id);
        return new NodeId(id);
    }

    public byte[] getBytes() {
        return id;
    }

    public NodeId middle(NodeId other) {
        if (other == null) {
            throw new IllegalArgumentException("Other node ID cannot be null");
        }

        BigInteger distance = distanceTo(other);
        BigInteger halfDistance = distance.shiftRight(1);
        BigInteger inBetween = toBigInteger().add(halfDistance);
        return fromBigInteger(inBetween);
    }

    public BigInteger distanceTo(NodeId other) {
        if (other == null) {
            throw new IllegalArgumentException("Other node ID cannot be null");
        }

        return toBigInteger().xor(other.toBigInteger());
    }

    public BigInteger toBigInteger() {
        return new BigInteger(1, id);
    }

    public static NodeId fromBigInteger(BigInteger id) {
        if (id == null) {
            throw new IllegalArgumentException("Node ID cannot be null");
        }

        if (id.bitLength() > SIZE) {
            throw new IllegalArgumentException("Node ID cannot be longer than 160 bits");
        }

        if (id.signum() == -1) {
            throw new IllegalArgumentException("Node ID cannot be negative");
        }

        byte[] bytes = id.toByteArray();

        if (bytes.length > BYTES) {
            // Remove extra leading byte used for the sign bit
            // Only required if the magnitude occupies all 160 bits, in which case the byte array will be 21 bytes long
            // If the magnitude is less than 160 bits, the byte array will be shorter than 21 bytes, and the sign bit
            // will be 0, so the extra leading byte will be 0 and can be ignored
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        } else if (bytes.length < BYTES) {
            // Pad with leading zeros
            byte[] padded = new byte[BYTES];
            System.arraycopy(bytes, 0, padded, BYTES - bytes.length, bytes.length);
            bytes = padded;
        }

        return new NodeId(bytes);
    }

    /**
     * Checks if this node ID is within the range of the given min and max node IDs.
     *
     * @param min The minimum node ID (inclusive).
     * @param max The maximum node ID (exclusive).
     * @return True if this node ID is within the range, false otherwise.
     */
    public boolean isWithinRange(NodeId min, NodeId max) {
        if (min == null) {
            throw new IllegalArgumentException("Min node ID cannot be null");
        }

        if (max == null) {
            throw new IllegalArgumentException("Max node ID cannot be null");
        }

        if (min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("Min node ID must be less than max node ID");
        }

        return compareTo(min) >= 0 && compareTo(max) < 0;
    }

    @Override
    public int compareTo(NodeId o) {
        return toBigInteger().compareTo(o.toBigInteger());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeId nodeId = (NodeId) o;
        return Arrays.equals(id, nodeId.id);
    }

    @Override
    public String toString() {
        return new String(id, StandardCharsets.UTF_8);
    }
}
