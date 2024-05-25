package jtorrent.domain.torrent.model;

import java.nio.file.Path;

public record FileMetadata(Path path, long start, long size, int firstPiece, int firstPieceStart, int lastPiece,
                           int lastPieceEnd) {

    public int numPieces() {
        return lastPiece - firstPiece + 1;
    }

    public long end() {
        return start + size - 1;
    }
}
