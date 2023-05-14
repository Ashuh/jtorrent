package jtorrent.domain.model.peer.message.typed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public abstract class BlockMessage extends TypedPeerMessage {

    private static final int PAYLOAD_BYTES = 12;

    /**
     * Zero-based piece index
     */
    protected final int index;

    /**
     * Zero-based byte offset within the piece
     */
    protected final int begin;

    /**
     * Requested length
     */
    protected final int length;

    protected BlockMessage(int index, int begin, int length) {
        this.index = index;
        this.begin = begin;
        this.length = length;
    }

    @Override
    protected byte[] getPayload() {
        return ByteBuffer.allocate(PAYLOAD_BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(index)
                .putInt(begin)
                .putInt(length)
                .array();
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, begin, length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlockMessage that = (BlockMessage) o;
        return index == that.index && begin == that.begin && length == that.length;
    }
}
