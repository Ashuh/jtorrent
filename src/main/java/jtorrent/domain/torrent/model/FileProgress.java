package jtorrent.domain.torrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class FileProgress {

    private final FileInfo fileInfo;
    private final FileMetadata fileMetaData;
    private final AtomicLong verifiedBytes;
    private final BehaviorSubject<Long> verifiedBytesSubject;
    private final BitSet verifiedPieces;
    private final BehaviorSubject<BitSet> verifiedPiecesSubject;

    public FileProgress(FileInfo fileInfo, FileMetadata fileMetaData, long verifiedBytes, BitSet verifiedPieces) {
        this.fileInfo = requireNonNull(fileInfo);
        this.fileMetaData = requireNonNull(fileMetaData);
        this.verifiedBytes = new AtomicLong(verifiedBytes);
        this.verifiedBytesSubject = BehaviorSubject.createDefault(verifiedBytes);
        this.verifiedPieces = requireNonNull(verifiedPieces);
        this.verifiedPiecesSubject = BehaviorSubject.createDefault((BitSet) verifiedPieces.clone());
    }

    public static FileProgress createNew(FileInfo fileInfo, FileMetadata fileMetaData) {
        return new FileProgress(fileInfo, fileMetaData, 0, new BitSet());
    }

    /**
     * @param fileInfo       the file info for the torrent
     * @param fileMetaData   the metadata for the file
     * @param verifiedPieces a bitset containing the verified pieces indices for the entire torrent, i.e.,
     *                       indices are global, not relative to the file
     */
    public static FileProgress createExisting(FileInfo fileInfo, FileMetadata fileMetaData,
            BitSet verifiedPieces) {
        long verifiedBytes = IntStream.range(fileMetaData.firstPiece(), fileMetaData.lastPiece() + 1)
                .filter(verifiedPieces::get)
                .mapToLong(piece -> getPieceBytesInFile(fileInfo, fileMetaData, piece))
                .sum();
        BitSet relativeVerifiedPieces = new BitSet();
        IntStream.range(fileMetaData.firstPiece(), fileMetaData.lastPiece() + 1)
                .filter(verifiedPieces::get)
                .forEach(piece -> relativeVerifiedPieces.set(piece - fileMetaData.firstPiece()));
        return new FileProgress(fileInfo, fileMetaData, verifiedBytes, relativeVerifiedPieces);
    }

    private static long getPieceBytesInFile(FileInfo fileInfo, FileMetadata fileMetaData, int piece) {
        long pieceStart = fileInfo.getPieceOffset(piece);
        long pieceEnd = pieceStart + fileInfo.getPieceSize(piece) - 1;
        long pieceStartWithinFile = Math.max(pieceStart, fileMetaData.start());
        long pieceEndWithinFile = Math.min(pieceEnd, fileMetaData.end());
        return pieceEndWithinFile - pieceStartWithinFile + 1;
    }

    private long getPieceBytesInFile(int piece) {
        return getPieceBytesInFile(fileInfo, fileMetaData, piece);
    }

    public long getVerifiedBytes() {
        return verifiedBytes.get();
    }

    public BitSet getVerifiedPieces() {
        return verifiedPieces;
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

    private void incrementVerifiedBytes(long bytes) {
        verifiedBytesSubject.onNext(verifiedBytes.addAndGet(bytes));
    }

    private int getRelativePieceIndex(int piece) {
        return piece - fileMetaData.firstPiece();
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

    @Override
    public int hashCode() {
        int result = fileInfo.hashCode();
        result = 31 * result + fileMetaData.hashCode();
        result = 31 * result + verifiedBytes.hashCode();
        result = 31 * result + verifiedPieces.hashCode();
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

        FileProgress that = (FileProgress) o;
        return fileInfo.equals(that.fileInfo)
                && fileMetaData.equals(that.fileMetaData)
                && (verifiedBytes.get() == that.verifiedBytes.get())
                && verifiedPieces.equals(that.verifiedPieces);
    }

    @Override
    public String toString() {
        return "FileProgress{"
                + "fileInfo=" + fileInfo
                + ", fileMetaData=" + fileMetaData
                + ", verifiedBytes=" + verifiedBytes
                + ", verifiedPieces=" + verifiedPieces
                + '}';
    }
}
