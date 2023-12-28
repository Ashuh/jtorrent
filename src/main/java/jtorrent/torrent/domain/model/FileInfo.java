package jtorrent.torrent.domain.model;

/**
 * @param firstPiece      the first piece index (zero-based, inclusive)
 * @param firstPieceStart the start offset in the first piece (zero-based, inclusive), i.e., where the file starts
 *                        in the first piece
 * @param lastPiece       the last piece index (zero-based, inclusive)
 * @param lastPieceEnd    the end offset in the last piece (zero-based, inclusive), i.e., where the file ends
 *                        in the last piece
 * @param start           the start offset of the file (zero-based, inclusive), i.e., the first byte of the file
 * @param end             the end offset of the file (zero-based, inclusive), i.e., the last byte of the file
 */
public record FileInfo(int firstPiece, int firstPieceStart, int lastPiece, int lastPieceEnd, long start, long end) {
}
