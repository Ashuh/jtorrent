package jtorrent.domain.torrent.model;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class FileProgress {

    private final FileInfo fileInfo;
    private final FileMetadata fileMetaData;
    private final AtomicLong verifiedBytes = new AtomicLong(0L);
    private final BehaviorSubject<Long> verifiedBytesSubject = BehaviorSubject.createDefault(0L);
    private final BitSet verifiedPieces = new BitSet();
    private final BehaviorSubject<BitSet> verifiedPiecesSubject = BehaviorSubject.createDefault(new BitSet());

    public FileProgress(FileInfo fileInfo, FileMetadata fileMetaData) {
        this.fileInfo = fileInfo;
        this.fileMetaData = fileMetaData;
    }

    private void incrementVerifiedBytes(long bytes) {
        verifiedBytesSubject.onNext(verifiedBytes.addAndGet(bytes));
    }

    public Observable<Long> getVerifiedBytesObservable() {
        return verifiedBytesSubject;
    }

    public Observable<BitSet> getVerifiedPiecesObservable() {
        return verifiedPiecesSubject;
    }

    public void setPieceVerified(int piece) {
        int relativePiece = getRelativePieceIndex(piece);
        if (verifiedPieces.get(relativePiece)) {
            return;
        }
        verifiedPieces.set(relativePiece);
        verifiedPiecesSubject.onNext((BitSet) verifiedPieces.clone());
        incrementVerifiedBytes(getPieceBytesInFile(piece));
    }

    public void setPieceNotVerified(int piece) {
        int relativePiece = getRelativePieceIndex(piece);
        if (!verifiedPieces.get(relativePiece)) {
            return;
        }
        verifiedPieces.clear(relativePiece);
        verifiedPiecesSubject.onNext((BitSet) verifiedPieces.clone());
        incrementVerifiedBytes(-getPieceBytesInFile(piece));
    }

    private long getPieceBytesInFile(int piece) {
        long pieceStart = fileInfo.getPieceOffset(piece);
        long pieceEnd = pieceStart + fileInfo.getPieceSize(piece) - 1;
        long pieceStartWithinFile = Math.max(pieceStart, fileMetaData.start());
        long pieceEndWithinFile = Math.min(pieceEnd, fileMetaData.end());
        return pieceEndWithinFile - pieceStartWithinFile + 1;
    }

    private int getRelativePieceIndex(int piece) {
        return piece - fileMetaData.firstPiece();
    }
}
