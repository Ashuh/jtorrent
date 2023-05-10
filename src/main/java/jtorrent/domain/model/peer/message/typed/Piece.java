package jtorrent.domain.model.peer.message.typed;

import static java.util.Objects.requireNonNull;

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
    protected MessageType getMessageType() {
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

    @Override
    public String toString() {
        return "Piece{" +
                "index=" + index +
                ", begin=" + begin +
                '}';
    }
}
