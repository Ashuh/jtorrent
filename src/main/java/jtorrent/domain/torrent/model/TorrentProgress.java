package jtorrent.domain.torrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class TorrentProgress {

    private final FileInfo fileInfo;
    private final Map<Path, FileProgress> pathToFileProgress;

    private final AtomicLong verifiedBytes;
    private final BehaviorSubject<Long> verifiedBytesSubject;
    private final BehaviorSubject<Long> checkedBytesSubject;
    private final BitSet completePieces;
    private final BitSet verifiedPieces;
    private final BehaviorSubject<BitSet> verifiedPiecesSubject;
    private final BehaviorSubject<BitSet> availablePiecesSubject = BehaviorSubject.createDefault(new BitSet());
    private final Map<Integer, BitSet> pieceIndexToRequestedBlocks = new HashMap<>();
    private final Map<Integer, BitSet> pieceIndexToAvailableBlocks;
    private final BitSet partiallyMissingPieces;
    private final BitSet partiallyMissingPiecesWithUnrequestedBlocks;
    private final BitSet completelyMissingPieces;
    private final BitSet completelyMissingPiecesWithUnrequestedBlocks;
    private long checkedBytes;

    public TorrentProgress(FileInfo fileInfo, Map<Path, FileProgress> pathToFileProgress, long verifiedBytes,
            BitSet completePieces, BitSet verifiedPieces, Map<Integer, BitSet> pieceIndexToAvailableBlocks,
            BitSet partiallyMissingPieces, BitSet partiallyMissingPiecesWithUnrequestedBlocks,
            BitSet completelyMissingPieces, BitSet completelyMissingPiecesWithUnrequestedBlocks, long checkedBytes) {
        this.fileInfo = requireNonNull(fileInfo);
        this.pathToFileProgress = pathToFileProgress;
        this.verifiedBytes = new AtomicLong(verifiedBytes);
        this.verifiedBytesSubject = BehaviorSubject.createDefault(verifiedBytes);
        this.checkedBytesSubject = BehaviorSubject.createDefault(checkedBytes);
        this.completePieces = completePieces;
        this.verifiedPieces = verifiedPieces;
        this.verifiedPiecesSubject = BehaviorSubject.createDefault((BitSet) verifiedPieces.clone());
        this.pieceIndexToAvailableBlocks = pieceIndexToAvailableBlocks;
        this.partiallyMissingPieces = partiallyMissingPieces;
        this.partiallyMissingPiecesWithUnrequestedBlocks = partiallyMissingPiecesWithUnrequestedBlocks;
        this.completelyMissingPieces = completelyMissingPieces;
        this.completelyMissingPiecesWithUnrequestedBlocks = completelyMissingPiecesWithUnrequestedBlocks;
        this.checkedBytes = checkedBytes;
    }

    public static TorrentProgress createNew(FileInfo fileInfo) {
        Map<Path, FileProgress> pathToFileProgress = new HashMap<>();
        fileInfo.getFileMetaData()
                .forEach(fileMetaData -> pathToFileProgress.put(fileMetaData.path(),
                        FileProgress.createNew(fileInfo, fileMetaData)));
        BitSet completePieces = new BitSet();
        BitSet verifiedPieces = new BitSet();
        Map<Integer, BitSet> pieceIndexToAvailableBlocks = new HashMap<>();
        BitSet partiallyMissingPieces = new BitSet();
        BitSet partiallyMissingPiecesWithUnrequestedBlocks = new BitSet();
        BitSet completelyMissingPieces = new BitSet();
        completelyMissingPieces.set(0, fileInfo.getNumPieces());
        BitSet completelyMissingPiecesWithUnrequestedBlocks = new BitSet();
        completelyMissingPiecesWithUnrequestedBlocks.set(0, fileInfo.getNumPieces());
        return new TorrentProgress(fileInfo, pathToFileProgress, 0, completePieces, verifiedPieces,
                pieceIndexToAvailableBlocks, partiallyMissingPieces,
                partiallyMissingPiecesWithUnrequestedBlocks, completelyMissingPieces,
                completelyMissingPiecesWithUnrequestedBlocks, 0);
    }

    public static TorrentProgress createExisting(FileInfo fileInfo, Map<Path, FileProgress> pathToFileProgress,
            BitSet verifiedPieces, Map<Integer, BitSet> pieceIndexToAvailableBlocks) {

        long verifiedBytes = verifiedPieces.stream()
                .mapToLong(fileInfo::getPieceSize)
                .sum();

        BitSet completePieces = new BitSet();
        BitSet partiallyMissingPieces = new BitSet();
        BitSet completelyMissingPieces = new BitSet();

        IntStream.range(0, fileInfo.getNumPieces())
                .forEach(piece -> {
                    final int numAvailableBlocks;
                    if (pieceIndexToAvailableBlocks.containsKey(piece)) {
                        numAvailableBlocks = pieceIndexToAvailableBlocks.get(piece).cardinality();
                    } else {
                        numAvailableBlocks = 0;
                    }

                    if (numAvailableBlocks == fileInfo.getNumBlocks(piece)) {
                        completePieces.set(piece);
                    } else if (numAvailableBlocks > 0) {
                        partiallyMissingPieces.set(piece);
                    } else {
                        completelyMissingPieces.set(piece);
                    }
                });

        BitSet partiallyMissingPiecesWithUnrequestedBlocks = (BitSet) partiallyMissingPieces.clone();
        BitSet completelyMissingPiecesWithUnrequestedBlocks = (BitSet) completelyMissingPieces.clone();

        return new TorrentProgress(fileInfo, pathToFileProgress, verifiedBytes, completePieces, verifiedPieces,
                pieceIndexToAvailableBlocks, partiallyMissingPieces,
                partiallyMissingPiecesWithUnrequestedBlocks, completelyMissingPieces,
                completelyMissingPiecesWithUnrequestedBlocks, 0);
    }

    public Map<Path, FileProgress> getFileProgress() {
        return pathToFileProgress;
    }

    public FileProgress getFileProgress(Path path) {
        return pathToFileProgress.get(path);
    }

    public Observable<Long> getVerifiedBytesObservable() {
        return verifiedBytesSubject;
    }

    public Observable<Long> getCheckedBytesObservable() {
        return checkedBytesSubject;
    }

    public long getVerifiedBytes() {
        return verifiedBytes.get();
    }

    public synchronized void setPieceChecked(int piece) {
        checkedBytes += fileInfo.getPieceSize(piece);
        checkedBytesSubject.onNext(checkedBytes);
    }

    public synchronized void resetCheckedBytes() {
        checkedBytes = 0;
        checkedBytesSubject.onNext(checkedBytes);
    }

    public synchronized void setPieceVerified(int piece) {
        if (isPieceVerified(piece)) {
            return;
        }

        completelyMissingPieces.clear(piece);
        completelyMissingPiecesWithUnrequestedBlocks.clear(piece);
        partiallyMissingPieces.clear(piece);
        partiallyMissingPiecesWithUnrequestedBlocks.clear(piece);
        completePieces.set(piece);
        verifiedPieces.set(piece);
        verifiedPiecesSubject.onNext((BitSet) verifiedPieces.clone());
        incrementVerified(fileInfo.getPieceSize(piece));

        long pieceStart = fileInfo.getPieceOffset(piece);
        long pieceEnd = pieceStart + fileInfo.getPieceSize(piece) - 1;
        List<FileMetadata> filesInRange = fileInfo.getInRange(pieceStart, pieceEnd);

        filesInRange.stream()
                .map(FileMetadata::path)
                .map(pathToFileProgress::get)
                .forEach(fileProgress -> fileProgress.setPieceVerified(piece));
    }

    private void incrementVerified(int amount) {
        verifiedBytesSubject.onNext(verifiedBytes.addAndGet(amount));
    }

    public synchronized boolean isPieceVerified(int piece) {
        return verifiedPieces.get(piece);
    }

    public synchronized void setPieceMissing(int piece) {
        if (isPieceVerified(piece)) {
            incrementVerified(-fileInfo.getPieceSize(piece));

            long pieceStart = fileInfo.getPieceOffset(piece);
            long pieceEnd = pieceStart + fileInfo.getPieceSize(piece) - 1;
            List<FileMetadata> filesInRange = fileInfo.getInRange(pieceStart, pieceEnd);

            filesInRange.stream()
                    .map(FileMetadata::path)
                    .map(pathToFileProgress::get)
                    .forEach(fileProgress -> fileProgress.setPieceNotVerified(piece));
        }

        verifiedPieces.clear(piece);
        verifiedPiecesSubject.onNext((BitSet) verifiedPieces.clone());
        completePieces.clear(piece);
        getAvailableBlocks(piece).clear();
        partiallyMissingPieces.clear(piece);
        partiallyMissingPiecesWithUnrequestedBlocks.clear(piece);
        completelyMissingPieces.set(piece);

        if (hasUnavailableAndUnrequestedBlocks(piece)) {
            completelyMissingPiecesWithUnrequestedBlocks.set(piece);
        } else {
            completelyMissingPiecesWithUnrequestedBlocks.clear(piece);
        }
    }

    /**
     * Checks whether the piece has any blocks that are both unavailable and unrequested.
     *
     * @param piece the piece index to check
     * @return true if the piece has any blocks that are both unavailable and unrequested, false otherwise
     */
    private boolean hasUnavailableAndUnrequestedBlocks(int piece) {
        BitSet unavailableBlocks = getUnavailableBlocks(piece);
        BitSet unrequestedBlocks = getUnrequestedBlocks(piece);
        return unavailableBlocks.intersects(unrequestedBlocks);
    }

    private BitSet getUnavailableBlocks(int piece) {
        BitSet unavailableBlocks = new BitSet();
        unavailableBlocks.set(0, fileInfo.getNumBlocks(piece));
        unavailableBlocks.andNot(getAvailableBlocks(piece));
        return unavailableBlocks;
    }

    public Map<Integer, BitSet> getReceivedBlocks() {
        return pieceIndexToAvailableBlocks;
    }

    private BitSet getAvailableBlocks(int piece) {
        return pieceIndexToAvailableBlocks.computeIfAbsent(piece, k -> new BitSet());
    }

    private BitSet getUnrequestedBlocks(int piece) {
        BitSet unrequestedBlocks = new BitSet();
        unrequestedBlocks.set(0, fileInfo.getNumBlocks(piece));
        unrequestedBlocks.andNot(getRequestedBlocks(piece));
        return unrequestedBlocks;
    }

    private BitSet getRequestedBlocks(int piece) {
        return pieceIndexToRequestedBlocks.computeIfAbsent(piece, k -> new BitSet());
    }

    public synchronized void setBlockRequested(int pieceIndex, int blockIndex) {
        BitSet requestedBlocks = getRequestedBlocks(pieceIndex);
        requestedBlocks.set(blockIndex);

        if (hasUnavailableAndUnrequestedBlocks(pieceIndex)) {
            return;
        }

        if (isPieceCompletelyMissing(pieceIndex)) {
            completelyMissingPiecesWithUnrequestedBlocks.clear(pieceIndex);
        } else if (isPiecePartiallyMissing(pieceIndex)) {
            partiallyMissingPiecesWithUnrequestedBlocks.clear(pieceIndex);
        }
    }

    private boolean isPieceCompletelyMissing(int piece) {
        return completelyMissingPieces.get(piece);
    }

    private boolean isPiecePartiallyMissing(int piece) {
        return partiallyMissingPieces.get(piece);
    }

    public synchronized void setBlockNotRequested(int pieceIndex, int blockIndex) {
        BitSet requestedBlocks = getRequestedBlocks(pieceIndex);
        requestedBlocks.clear(blockIndex);

        if (isPieceCompletelyMissing(pieceIndex) && hasUnavailableAndUnrequestedBlocks(pieceIndex)) {
            completelyMissingPiecesWithUnrequestedBlocks.set(pieceIndex);
        } else if (isPiecePartiallyMissing(pieceIndex) && hasUnavailableAndUnrequestedBlocks(pieceIndex)) {
            partiallyMissingPiecesWithUnrequestedBlocks.set(pieceIndex);
        }
    }

    public synchronized void setBlockReceived(int pieceIndex, int blockIndex) {
        if (isBlockAvailable(pieceIndex, blockIndex)) {
            return;
        }

        boolean isAllBlocksReceived = setBlockAvailable(pieceIndex, blockIndex);

        if (isAllBlocksReceived) {
            setPieceComplete(pieceIndex);
        } else {
            setPiecePartiallyMissing(pieceIndex);
        }
    }

    private boolean isBlockAvailable(int piece, int blockIndex) {
        return getAvailableBlocks(piece).get(blockIndex);
    }

    private void setPieceComplete(int piece) {
        completePieces.set(piece);
        completelyMissingPieces.clear(piece);
        completelyMissingPiecesWithUnrequestedBlocks.clear(piece);
        partiallyMissingPieces.clear(piece);
        partiallyMissingPiecesWithUnrequestedBlocks.clear(piece);
    }

    private void setPiecePartiallyMissing(int piece) {
        completelyMissingPieces.clear(piece);
        completelyMissingPiecesWithUnrequestedBlocks.clear(piece);
        partiallyMissingPieces.set(piece);

        if (hasUnavailableAndUnrequestedBlocks(piece)) {
            partiallyMissingPiecesWithUnrequestedBlocks.set(piece);
        } else {
            partiallyMissingPiecesWithUnrequestedBlocks.clear(piece);
        }
    }

    /**
     * Sets the block as available and returns whether all blocks for the piece are now available.
     *
     * @param piece the piece index
     * @param block the block index
     * @return true if all blocks for the piece are now available, false otherwise
     */
    private boolean setBlockAvailable(int piece, int block) {
        BitSet availableBlocks = getAvailableBlocks(piece);
        availableBlocks.set(block);
        return availableBlocks.cardinality() == fileInfo.getNumBlocks(piece);
    }

    public synchronized boolean isPieceComplete(int piece) {
        return completePieces.get(piece);
    }

    public synchronized boolean isAllPiecesVerified() {
        return verifiedPieces.cardinality() == fileInfo.getNumPieces();
    }

    public synchronized BitSet getVerifiedPieces() {
        return (BitSet) verifiedPieces.clone();
    }

    public Observable<BitSet> getVerifiedPiecesObservable() {
        return verifiedPiecesSubject;
    }

    public Observable<BitSet> getAvailablePiecesObservable() {
        return availablePiecesSubject;
    }

    public synchronized BitSet getPartiallyMissingPiecesWithUnrequestedBlocks() {
        return (BitSet) partiallyMissingPiecesWithUnrequestedBlocks.clone();
    }

    public synchronized BitSet getCompletelyMissingPiecesWithUnrequestedBlocks() {
        return (BitSet) completelyMissingPiecesWithUnrequestedBlocks.clone();
    }

    public synchronized BitSet getMissingBlocks(int piece) {
        BitSet requestedBlocks = getRequestedBlocks(piece);
        BitSet availableBlocks = getAvailableBlocks(piece);
        BitSet unavailableAndUnrequestedBlocks = new BitSet();
        unavailableAndUnrequestedBlocks.set(0, fileInfo.getNumBlocks(piece));
        unavailableAndUnrequestedBlocks.andNot(availableBlocks);
        unavailableAndUnrequestedBlocks.andNot(requestedBlocks);
        return unavailableAndUnrequestedBlocks;
    }

    private boolean hasUnrequestedBlocks(int piece) {
        return getRequestedBlocks(piece).cardinality() < fileInfo.getNumBlocks(piece);
    }

    private boolean hasUnavailableBlocks(int piece) {
        return getAvailableBlocks(piece).cardinality() < fileInfo.getNumBlocks(piece);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TorrentProgress that = (TorrentProgress) o;
        return checkedBytes == that.checkedBytes
                && fileInfo.equals(that.fileInfo)
                && pathToFileProgress.equals(that.pathToFileProgress)
                && (verifiedBytes.get() == that.verifiedBytes.get())
                && completePieces.equals(that.completePieces)
                && verifiedPieces.equals(that.verifiedPieces)
                && pieceIndexToRequestedBlocks.equals(that.pieceIndexToRequestedBlocks)
                && pieceIndexToAvailableBlocks.equals(that.pieceIndexToAvailableBlocks)
                && partiallyMissingPieces.equals(that.partiallyMissingPieces)
                && partiallyMissingPiecesWithUnrequestedBlocks.equals(that.partiallyMissingPiecesWithUnrequestedBlocks)
                && completelyMissingPieces.equals(that.completelyMissingPieces)
                && completelyMissingPiecesWithUnrequestedBlocks.equals(
                that.completelyMissingPiecesWithUnrequestedBlocks);
    }

    @Override
    public int hashCode() {
        int result = fileInfo.hashCode();
        result = 31 * result + pathToFileProgress.hashCode();
        result = 31 * result + verifiedBytes.hashCode();
        result = 31 * result + completePieces.hashCode();
        result = 31 * result + verifiedPieces.hashCode();
        result = 31 * result + pieceIndexToRequestedBlocks.hashCode();
        result = 31 * result + pieceIndexToAvailableBlocks.hashCode();
        result = 31 * result + partiallyMissingPieces.hashCode();
        result = 31 * result + partiallyMissingPiecesWithUnrequestedBlocks.hashCode();
        result = 31 * result + completelyMissingPieces.hashCode();
        result = 31 * result + completelyMissingPiecesWithUnrequestedBlocks.hashCode();
        result = 31 * result + Long.hashCode(checkedBytes);
        return result;
    }

    @Override
    public String toString() {
        return "TorrentProgress{"
                + "fileInfo=" + fileInfo
                + ", pathToFileProgress=" + pathToFileProgress
                + ", verifiedBytes=" + verifiedBytes
                + ", completePieces=" + completePieces
                + ", verifiedPieces=" + verifiedPieces
                + ", pieceIndexToRequestedBlocks=" + pieceIndexToRequestedBlocks
                + ", pieceIndexToAvailableBlocks=" + pieceIndexToAvailableBlocks
                + ", partiallyMissingPieces=" + partiallyMissingPieces
                + ", partiallyMissingPiecesWithUnrequestedBlocks=" + partiallyMissingPiecesWithUnrequestedBlocks
                + ", completelyMissingPieces=" + completelyMissingPieces
                + ", completelyMissingPiecesWithUnrequestedBlocks=" + completelyMissingPiecesWithUnrequestedBlocks
                + ", checkedBytes=" + checkedBytes
                + '}';
    }
}
