package jtorrent.domain.torrent.model;

import java.nio.file.Path;

public record FileMetadata(long size, Path path, int firstPiece, int firstPieceStart, int lastPiece, int lastPieceEnd,
                           long start) {

    public int numPieces() {
        return lastPiece - firstPiece + 1;
    }

    public long end() {
        return start + size - 1;
    }
}
