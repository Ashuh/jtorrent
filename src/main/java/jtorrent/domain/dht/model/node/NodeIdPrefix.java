package jtorrent.domain.dht.model.node;

import static jtorrent.domain.common.util.ValidationUtil.requireInRange;

import java.util.Objects;

import jtorrent.domain.common.util.Bit160Value;
import jtorrent.domain.common.util.ByteArray;

/**
 * Represents the prefix of a {@link NodeId}.
 */
public class NodeIdPrefix extends ByteArray {

    private final int bitLength;

    public NodeIdPrefix(byte[] bytes, int bitLength) {
        super(bytes);
        requireInRange(bitLength, 0, Math.min(Bit160Value.SIZE_BITS, bytes.length * Byte.SIZE));
        this.bitLength = bitLength;
    }

    public int getBitLength() {
        return bitLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NodeIdPrefix that = (NodeIdPrefix) o;
        return bitLength == that.bitLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bitLength);
    }
}
