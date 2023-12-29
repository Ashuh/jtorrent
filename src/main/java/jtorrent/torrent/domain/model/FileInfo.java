package jtorrent.torrent.domain.model;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public final class FileInfo {

    private final int firstPiece;
    private final int firstPieceStart;
    private final int lastPiece;
    private final int lastPieceEnd;
    private final long start;
    private final long end;
    private final AtomicLong verifiedBytes = new AtomicLong(0L);
    private final BehaviorSubject<Long> verifiedBytesSubject = BehaviorSubject.createDefault(0L);
    private final BitSet verifiedPieces = new BitSet();
    private final BehaviorSubject<BitSet> verifiedPiecesSubject = BehaviorSubject.createDefault(new BitSet());

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
    public FileInfo(int firstPiece, int firstPieceStart, int lastPiece, int lastPieceEnd, long start, long end) {
        this.firstPiece = firstPiece;
        this.firstPieceStart = firstPieceStart;
        this.lastPiece = lastPiece;
        this.lastPieceEnd = lastPieceEnd;
        this.start = start;
        this.end = end;
    }

    public int numPieces() {
        return lastPiece - firstPiece + 1;
    }

    public int firstPiece() {
        return firstPiece;
    }

    public int firstPieceStart() {
        return firstPieceStart;
    }

    public int lastPiece() {
        return lastPiece;
    }

    public int lastPieceEnd() {
        return lastPieceEnd;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public void incrementVerifiedBytes(long bytes) {
        verifiedBytesSubject.onNext(verifiedBytes.addAndGet(bytes));
    }

    public Observable<Long> getVerifiedBytesObservable() {
        return verifiedBytesSubject;
    }

    public void setPieceVerified(int piece) {
        verifiedPieces.set(piece);
        verifiedPiecesSubject.onNext((BitSet) verifiedPieces.clone());
    }

    public void setPieceNotVerified(int piece) {
        verifiedPieces.clear(piece);
        verifiedPiecesSubject.onNext(verifiedPieces);
    }

    public Observable<BitSet> getVerifiedPiecesObservable() {
        return verifiedPiecesSubject;
    }
}
