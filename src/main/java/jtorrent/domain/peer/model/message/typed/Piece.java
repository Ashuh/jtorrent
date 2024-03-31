package jtorrent.domain.peer.model.message.typed;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class Piece extends TypedPeerMessage {

    /**
     * Zero-based piece index
     */
    private final int index;

    /**
     * Zero-based byte offset within the piece
     */
    private final int begin;

    /**
     * Block of data
     */
    private final byte[] block;

    public Piece(int index, int begin, byte[] block) {
        this.index = index;
        this.begin = begin;
        this.block = requireNonNull(block);
    }

    public static Piece unpack(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int index = buffer.getInt();
        int begin = buffer.getInt();
        byte[] block = new byte[buffer.remaining()];
        buffer.get(block);
        return new Piece(index, begin, block);
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public byte[] getBlock() {
        return block;
    }

    @Override
    protected int getPayloadSize() {
        return Integer.BYTES * 2 + block.length;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.PIECE;
    }

    @Override
    protected byte[] getPayload() {
        return ByteBuffer.allocate(Integer.BYTES * 2 + block.length)
                .putInt(index)
                .putInt(begin)
                .put(block)
                .array();
    }

    @Override
    protected String getPayloadString() {
        return String.format("index=%d, begin=%d, block=%s bytes", index, begin, block.length);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(index, begin);
        result = 31 * result + Arrays.hashCode(block);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Piece piece = (Piece) o;
        return index == piece.index && begin == piece.begin && Arrays.equals(block, piece.block);
    }
}
