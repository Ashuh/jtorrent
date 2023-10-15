package jtorrent.domain.model.dht.node;

import java.util.Arrays;

import jtorrent.domain.util.Bit160Value;
import jtorrent.domain.util.ByteArray;

/**
 * Represents the prefix of a {@link NodeId}.
 */
public class NodeIdPrefix extends ByteArray {

    private NodeIdPrefix(byte[] bytes) {
        super(bytes);
    }

    /**
     * Creates a {@link NodeIdPrefix} from the given {@link NodeId} and bitLength.
     *
     * @param nodeId    The {@link NodeId} to create the prefix from
     * @param bitLength The number of bits to include in the prefix. Must be between 0 and 160 (inclusive).
     * @return the {@link NodeIdPrefix} created from the given {@link NodeId} and bitLength
     */
    public static NodeIdPrefix fromNodeId(NodeId nodeId, int bitLength) {
        if (nodeId == null) {
            throw new IllegalArgumentException("Node ID cannot be null");
        }

        if (bitLength < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }

        if (bitLength > Bit160Value.SIZE_BITS) {
            throw new IllegalArgumentException("Length cannot be greater than " + Bit160Value.SIZE_BITS + " bits");
        }

        int numFullBytes = bitLength / Byte.SIZE;
        int numTrailingBits = bitLength % Byte.SIZE;
        int numBytesToCopy = numFullBytes + (numTrailingBits > 0 ? 1 : 0);
        byte[] bytes = Arrays.copyOf(nodeId.getBytes(), numBytesToCopy);
        // Clear the trailing bits
        if (numTrailingBits > 0) {
            bytes[numFullBytes] &= (byte) (0xFF << (Byte.SIZE - numTrailingBits));
        }
        return new NodeIdPrefix(bytes);
    }
}
