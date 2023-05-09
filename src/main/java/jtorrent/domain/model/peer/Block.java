package jtorrent.domain.model.peer;

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
}
