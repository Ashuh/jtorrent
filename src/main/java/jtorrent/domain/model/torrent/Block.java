package jtorrent.domain.model.torrent;

import java.util.Objects;

public class Block {

    private final int index;
    private final int offset;
    private final int length;

    public Block(int index, int offset, int length) {
        this.index = index;
        this.offset = offset;
        this.length = length;
    }

    public int getIndex() {
        return index;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, offset, length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Block block = (Block) o;
        return index == block.index
                && offset == block.offset
                && length == block.length;
    }

    @Override
    public String toString() {
        return "Block{"
                + "index=" + index
                + ", offset=" + offset
                + ", length=" + length
                + '}';
    }
}
