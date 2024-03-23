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
    private final Map<Path, FileProgress> pathToFileState = new HashMap<>();

    private final AtomicLong verifiedBytes = new AtomicLong(0);
    private final BehaviorSubject<Long> verifiedBytesSubject = BehaviorSubject.createDefault(0L);
    private final BehaviorSubject<Long> checkedBytesSubject = BehaviorSubject.createDefault(0L);
    private final BitSet completePieces = new BitSet();
    private final BitSet verifiedPieces = new BitSet();
    private final BehaviorSubject<BitSet> verifiedPiecesSubject = BehaviorSubject.createDefault(new BitSet());
    private final BehaviorSubject<BitSet> availablePiecesSubject = BehaviorSubject.createDefault(new BitSet());
    private final Map<Integer, BitSet> pieceIndexToRequestedBlocks = new HashMap<>();
    private final Map<Integer, BitSet> pieceIndexToAvailableBlocks = new HashMap<>();
    private final BitSet partiallyMissingPieces = new BitSet();
    private final BitSet partiallyMissingPiecesWithUnrequestedBlocks = new BitSet();
    private final BitSet completelyMissingPieces = new BitSet();
    private final BitSet completelyMissingPiecesWithUnrequestedBlocks = new BitSet();
    private long checkedBytes = 0;

    public TorrentProgress(FileInfo fileInfo) {
        this.fileInfo = requireNonNull(fileInfo);

        IntStream.range(0, fileInfo.getNumPieces())
                .forEach(i -> {
                    pieceIndexToRequestedBlocks.put(i, new BitSet());
                    pieceIndexToAvailableBlocks.put(i, new BitSet());
                    completelyMissingPieces.set(i);
                    completelyMissingPiecesWithUnrequestedBlocks.set(i);
                });

        fileInfo.getFileMetaData()
                .forEach(fileMetaData -> pathToFileState.put(fileMetaData.path(),
                        new FileProgress(fileInfo, fileMetaData)));
    }

    public FileProgress getFileState(Path path) {
        return pathToFileState.get(path);
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
                .map(pathToFileState::get)
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
                    .map(pathToFileState::get)
                    .forEach(fileProgress -> fileProgress.setPieceNotVerified(piece));
        }

        verifiedPieces.clear(piece);
        verifiedPiecesSubject.onNext((BitSet) verifiedPieces.clone());
        completePieces.clear(piece);
        pieceIndexToAvailableBlocks.get(piece).clear();
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

    private BitSet getAvailableBlocks(int piece) {
        return pieceIndexToAvailableBlocks.get(piece);
    }

    private BitSet getUnrequestedBlocks(int piece) {
        BitSet unrequestedBlocks = new BitSet();
        unrequestedBlocks.set(0, fileInfo.getNumBlocks(piece));
        unrequestedBlocks.andNot(getRequestedBlocks(piece));
        return unrequestedBlocks;
    }

    private BitSet getRequestedBlocks(int piece) {
        return pieceIndexToRequestedBlocks.get(piece);
    }

    public synchronized void setBlockRequested(int pieceIndex, int blockIndex) {
        BitSet requestedBlocks = pieceIndexToRequestedBlocks.get(pieceIndex);
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
        BitSet requestedBlocks = pieceIndexToRequestedBlocks.get(pieceIndex);
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
        return pieceIndexToAvailableBlocks.get(piece).get(blockIndex);
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
        return pieceIndexToRequestedBlocks.get(piece).cardinality() < fileInfo.getNumBlocks(piece);
    }

    private boolean hasUnavailableBlocks(int piece) {
        return pieceIndexToAvailableBlocks.get(piece).cardinality() < fileInfo.getNumBlocks(piece);
    }
}
