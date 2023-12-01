package jtorrent.common.domain.model;

import java.util.Objects;

public class Block {

    private final int pieceIndex;
    private final int blockIndex;

    public Block(int pieceIndex, int blockIndex) {
        this.pieceIndex = pieceIndex;
        this.blockIndex = blockIndex;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceIndex, blockIndex);
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
        return pieceIndex == block.pieceIndex
                && blockIndex == block.blockIndex;
    }

    @Override
    public String toString() {
        return String.format("[Piece %d, Block %d]", pieceIndex, blockIndex);
    }
}
