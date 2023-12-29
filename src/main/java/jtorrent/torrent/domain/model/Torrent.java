package jtorrent.torrent.domain.model;

import static jtorrent.common.domain.util.ValidationUtil.requireAtMost;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNegative;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.common.domain.util.rx.CombinedDoubleSumObservable;
import jtorrent.common.domain.util.rx.MutableRxObservableSet;
import jtorrent.common.domain.util.rx.RxObservableSet;
import jtorrent.peer.domain.model.Peer;
import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.tracker.domain.handler.TrackerHandler;
import jtorrent.tracker.domain.model.Tracker;

public class Torrent implements TrackerHandler.TorrentProgressProvider {

    private static final int BLOCK_SIZE = 16384;

    private final Set<Tracker> trackers;
    private final LocalDateTime creationDate;
    private final String comment;
    private final String createdBy;
    private final int pieceSize;
    private final List<Sha1Hash> pieceHashes;
    private final String name;
    private final List<File> files;
    private final List<FileInfo> fileInfos;
    private final Sha1Hash infoHash;
    private final PieceTracker pieceTracker;
    private final AtomicInteger downloaded = new AtomicInteger(0);
    private final BehaviorSubject<Integer> downloadedSubject = BehaviorSubject.createDefault(0);
    private final AtomicInteger uploaded = new AtomicInteger(0);
    private final BehaviorSubject<Integer> uploadedSubject = BehaviorSubject.createDefault(0);
    private final MutableRxObservableSet<Peer> peers = new MutableRxObservableSet<>(new HashSet<>());
    private final CombinedDoubleSumObservable downloadRateObservable = new CombinedDoubleSumObservable();
    private final CombinedDoubleSumObservable uploadRateObservable = new CombinedDoubleSumObservable();
    private final AtomicLong verifiedBytes = new AtomicLong(0);
    private final BehaviorSubject<Long> verifiedBytesSubject = BehaviorSubject.createDefault(0L);
    private final BehaviorSubject<BitSet> verifiedPiecesSubject = BehaviorSubject.createDefault(new BitSet());
    private final BehaviorSubject<BitSet> availablePiecesSubject = BehaviorSubject.createDefault(new BitSet());
    private final BehaviorSubject<Boolean> isActiveSubject = BehaviorSubject.createDefault(false);
    private boolean isActive = false;

    public Torrent(Set<Tracker> trackers, LocalDateTime creationDate, String comment, String createdBy,
            int pieceSize, List<Sha1Hash> pieceHashes, String name, List<File> files, Sha1Hash infoHash) {
        this.trackers = requireNonNull(trackers);
        this.creationDate = requireNonNull(creationDate);
        this.comment = requireNonNull(comment);
        this.createdBy = requireNonNull(createdBy);
        this.pieceSize = pieceSize;
        this.pieceHashes = requireNonNull(pieceHashes);
        this.name = requireNonNull(name);
        this.files = requireNonNull(files);
        this.fileInfos = computeFileBoundaries(files, pieceSize);
        this.infoHash = requireNonNull(infoHash);
        this.pieceTracker = new PieceTracker();
    }

    private static List<FileInfo> computeFileBoundaries(List<File> files, long pieceSize) {
        List<FileInfo> fileInfos = new ArrayList<>();

        int prevLastPiece = 0; // inclusive
        int prevLastPieceEnd = -1; // inclusive

        for (File file : files) {
            int firstPiece = prevLastPiece;
            int firstPieceStart = prevLastPieceEnd + 1;
            boolean isPrevLastPieceFullyOccupied = firstPieceStart == pieceSize;
            if (isPrevLastPieceFullyOccupied) {
                firstPiece++;
                firstPieceStart = 0;
            }

            long fileStart = firstPiece * pieceSize + firstPieceStart;
            long firstPieceBytes = pieceSize - firstPieceStart;
            long remainingFileBytes = file.getSize() - firstPieceBytes;
            int numRemainingPieces = (int) Math.ceil(remainingFileBytes / (double) pieceSize);
            int lastPiece = firstPiece + numRemainingPieces;
            int lastPieceEnd = (int) (remainingFileBytes % pieceSize) - 1;
            long fileEnd = lastPiece * pieceSize + lastPieceEnd;

            prevLastPiece = lastPiece;
            prevLastPieceEnd = lastPieceEnd;
            FileInfo fileInfo = new FileInfo(firstPiece, firstPieceStart, lastPiece, lastPieceEnd, fileStart, fileEnd);
            fileInfos.add(fileInfo);
        }

        return fileInfos;
    }

    public Set<Tracker> getTrackers() {
        return trackers;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public int getPieceSize(int pieceIndex) {
        if (pieceIndex == getNumPieces() - 1) {
            int remainder = (int) (getTotalSize() % pieceSize);
            return remainder == 0 ? pieceSize : remainder;
        }
        return pieceSize;
    }

    public int getBlockSize() {
        return BLOCK_SIZE;
    }

    public int getBlockSize(int pieceIndex, int blockIndex) {
        if (blockIndex == getNumBlocks(pieceIndex) - 1) {
            int remainder = getPieceSize(pieceIndex) % BLOCK_SIZE;
            return remainder == 0 ? BLOCK_SIZE : remainder;
        }
        return BLOCK_SIZE;
    }

    public List<Sha1Hash> getPieceHashes() {
        return pieceHashes;
    }

    public Sha1Hash getPieceHash(int piece) {
        return pieceHashes.get(piece);
    }

    public String getName() {
        return name;
    }

    public List<File> getFiles() {
        return files;
    }

    public List<Map.Entry<File, FileInfo>> getFilesWithInfo() {
        return IntStream.range(0, files.size())
                .mapToObj(i -> Map.entry(files.get(i), fileInfos.get(i)))
                .collect(Collectors.toList());
    }

    @Override
    public Sha1Hash getInfoHash() {
        return infoHash;
    }

    @Override
    public long getDownloaded() {
        return downloaded.get();
    }

    @Override
    public long getUploaded() {
        return uploaded.get();
    }

    @Override
    public long getLeft() {
        return getTotalSize() - getVerifiedBytes();
    }

    public long getTotalSize() {
        return files.stream()
                .map(File::getSize)
                .mapToLong(Long::longValue)
                .sum();
    }

    private int getVerifiedBytes() {
        return pieceTracker.getVerifiedPieces()
                .stream()
                .map(this::getPieceSize)
                .sum();
    }

    public int getNumPieces() {
        return pieceHashes.size();
    }

    /**
     * Returns the files and their corresponding file info that fall within the given range.
     *
     * @param start the byte offset to start at (inclusive)
     * @param end   the byte offset to end at (inclusive)
     * @return a list of files and their corresponding file info that fall within the given range that is sorted by the
     * start byte offset of the file
     */
    public List<Map.Entry<File, FileInfo>> getFilesInRange(long start, long end) {
        int startIndex = getFileIndex(start); // inclusive
        int endIndex = getFileIndex(end); // inclusive

        return IntStream.range(startIndex, endIndex + 1)
                .mapToObj(i -> Map.entry(files.get(i), fileInfos.get(i)))
                .collect(Collectors.toList());
    }

    private int getFileIndex(long offset) {
        requireNonNegative(offset);
        requireAtMost(offset, getTotalSize() - 1);

        int low = 0;
        int high = files.size() - 1;

        while (low < high) {
            int mid = low + (high - low) / 2;
            FileInfo midFileInfo = fileInfos.get(mid);
            int midFileFirstPiece = midFileInfo.firstPiece();
            long midStart = getPieceOffset(midFileFirstPiece) + midFileInfo.firstPieceStart();
            int midFileLastPiece = midFileInfo.lastPiece();
            long midEnd = getPieceOffset(midFileLastPiece) + midFileInfo.lastPieceEnd();
            boolean isOffsetWithinMidFile = offset >= midStart && offset <= midEnd;

            if (isOffsetWithinMidFile) {
                return mid;
            } else if (offset < midStart) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return high;
    }

    public long getPieceOffset(int index) {
        return (long) getPieceSize() * index;
    }

    public void incrementDownloaded(int amount) {
        downloadedSubject.onNext(downloaded.addAndGet(amount));
    }

    public void incrementUploaded(int amount) {
        uploadedSubject.onNext(uploaded.addAndGet(amount));
    }

    public void setBlockReceived(int pieceIndex, int blockIndex) {
        pieceTracker.setBlockReceived(pieceIndex, blockIndex);
    }

    public void setBlockNotRequested(int pieceIndex, int blockIndex) {
        pieceTracker.setBlockNotRequested(pieceIndex, blockIndex);
    }

    public void setPieceMissing(int pieceIndex) {
        if (pieceTracker.isPieceVerified(pieceIndex)) {
            verifiedBytes.getAndAdd(-getPieceSize(pieceIndex));
            verifiedBytesSubject.onNext(verifiedBytes.get());
            verifiedPiecesSubject.onNext(pieceTracker.getVerifiedPieces());

            long pieceStart = getPieceOffset(pieceIndex);
            long pieceEnd = pieceStart + getPieceSize(pieceIndex) - 1;
            List<Map.Entry<File, FileInfo>> filesInRange = getFilesInRange(pieceStart, pieceEnd);

            for (Map.Entry<File, FileInfo> fileWithInfo : filesInRange) {
                FileInfo fileInfo = fileWithInfo.getValue();
                fileInfo.setPieceNotVerified(pieceIndex - fileInfo.firstPiece());
                long pieceBytesWithinFile = getPieceBytesInFile(pieceIndex, fileInfo);
                fileInfo.incrementVerifiedBytes(pieceBytesWithinFile);
            }
        }
        pieceTracker.setPieceMissing(pieceIndex);
    }

    private long getPieceBytesInFile(int piece, FileInfo fileInfo) {
        long pieceStart = getPieceOffset(piece);
        long pieceEnd = pieceStart + getPieceSize(piece) - 1;
        long pieceStartWithinFile = Math.max(pieceStart, fileInfo.start());
        long pieceEndWithinFile = Math.min(pieceEnd, fileInfo.end());
        return pieceEndWithinFile - pieceStartWithinFile + 1;
    }

    public BitSet getCompletelyMissingPiecesWithUnrequestedBlocks() {
        return pieceTracker.getCompletelyMissingPiecesWithUnrequestedBlocks();
    }

    public BitSet getPartiallyMissingPiecesWithUnrequestedBlocks() {
        return pieceTracker.getPartiallyMissingPiecesWithUnrequestedBlocks();
    }

    public BitSet getVerifiedPieces() {
        return pieceTracker.getVerifiedPieces();
    }

    public Observable<BitSet> getVerifiedPiecesObservable() {
        return verifiedPiecesSubject;
    }

    public Observable<BitSet> getAvailablePiecesObservable() {
        return availablePiecesSubject;
    }

    public void setPieceVerified(int pieceIndex) {
        if (pieceTracker.isPieceVerified(pieceIndex)) {
            return;
        }

        pieceTracker.setPieceVerified(pieceIndex);
        verifiedBytes.getAndAdd(getPieceSize(pieceIndex));
        verifiedBytesSubject.onNext(verifiedBytes.get());
        verifiedPiecesSubject.onNext(pieceTracker.getVerifiedPieces());

        long pieceStart = getPieceOffset(pieceIndex);
        long pieceEnd = pieceStart + getPieceSize(pieceIndex) - 1;
        List<Map.Entry<File, FileInfo>> filesInRange = getFilesInRange(pieceStart, pieceEnd);

        for (Map.Entry<File, FileInfo> fileWithInfo : filesInRange) {
            FileInfo fileInfo = fileWithInfo.getValue();
            fileInfo.setPieceVerified(pieceIndex - fileInfo.firstPiece());
            long pieceBytesWithinFile = getPieceBytesInFile(pieceIndex, fileInfo);
            fileInfo.incrementVerifiedBytes(pieceBytesWithinFile);
        }
    }

    public boolean isPieceComplete(int pieceIndex) {
        return pieceTracker.isPieceComplete(pieceIndex);
    }

    public boolean isAllPiecesVerified() {
        return pieceTracker.isAllPiecesVerified();
    }

    public BitSet getMissingBlocks(int pieceIndex) {
        return pieceTracker.getMissingBlocks(pieceIndex);
    }

    public void setBlockRequested(int pieceIndex, int blockIndex) {
        pieceTracker.setBlockRequested(pieceIndex, blockIndex);
    }

    private int getNumBlocks(int pieceIndex) {
        return (int) Math.ceil((double) getPieceSize(pieceIndex) / BLOCK_SIZE);
    }

    public Observable<Double> getDownloadRateObservable() {
        return downloadRateObservable;
    }

    public Observable<Integer> getDownloadedObservable() {
        return downloadedSubject;
    }

    public Observable<Double> getUploadRateObservable() {
        return uploadRateObservable;
    }

    public Observable<Integer> getUploadedObservable() {
        return uploadedSubject;
    }

    public Observable<Long> getVerifiedBytesObservable() {
        return verifiedBytesSubject;
    }

    public RxObservableSet<Peer> getPeersObservable() {
        return peers;
    }

    public void addPeer(Peer peer) {
        peers.add(peer);
        downloadRateObservable.addSource(peer.getDownloadRateObservable());
        uploadRateObservable.addSource(peer.getUploadRateObservable());
    }

    public void removePeer(Peer peer) {
        peers.remove(peer);
        downloadRateObservable.removeSource(peer.getDownloadRateObservable());
        uploadRateObservable.removeSource(peer.getUploadRateObservable());
    }

    public void clearPeers() {
        peers.clear();
        downloadRateObservable.clearSources();
        uploadRateObservable.clearSources();
    }

    public boolean hasPeer(PeerContactInfo peerContactInfo) {
        return peers.anyMatch(peer -> peer.getPeerContactInfo().equals(peerContactInfo));
    }

    public Observable<Boolean> getIsActiveObservable() {
        return isActiveSubject;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
        isActiveSubject.onNext(isActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackers, creationDate, comment, createdBy, pieceSize, pieceHashes, name, files, infoHash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Torrent torrent = (Torrent) o;
        return pieceSize == torrent.pieceSize
                && trackers.equals(torrent.trackers)
                && creationDate.equals(torrent.creationDate)
                && comment.equals(torrent.comment)
                && createdBy.equals(torrent.createdBy)
                && pieceHashes.equals(torrent.pieceHashes)
                && name.equals(torrent.name)
                && files.equals(torrent.files)
                && infoHash.equals(torrent.infoHash);
    }

    @Override
    public String toString() {
        return "Torrent{"
                + "trackers=" + trackers
                + ", creationDate=" + creationDate
                + ", comment='" + comment + '\''
                + ", createdBy='" + createdBy + '\''
                + ", pieceSize=" + pieceSize
                + ", name='" + name + '\''
                + ", files=" + files
                + ", infoHash=" + infoHash
                + '}';
    }

    private class PieceTracker {

        private final Map<Integer, BitSet> pieceIndexToRequestedBlocks = new HashMap<>();
        private final Map<Integer, BitSet> pieceIndexToAvailableBlocks = new HashMap<>();
        private final BitSet partiallyMissingPieces = new BitSet();
        private final BitSet partiallyMissingPiecesWithUnrequestedBlocks = new BitSet();
        private final BitSet completelyMissingPieces = new BitSet();
        private final BitSet completelyMissingPiecesWithUnrequestedBlocks = new BitSet();
        private final BitSet completePieces = new BitSet();
        private final BitSet verifiedPieces = new BitSet();

        public PieceTracker() {
            IntStream.range(0, getNumPieces())
                    .forEach(i -> {
                        pieceIndexToRequestedBlocks.put(i, new BitSet());
                        pieceIndexToAvailableBlocks.put(i, new BitSet());
                        completelyMissingPieces.set(i);
                        completelyMissingPiecesWithUnrequestedBlocks.set(i);
                    });
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

        public synchronized void setPieceVerified(int pieceIndex) {
            completelyMissingPieces.clear(pieceIndex);
            completelyMissingPiecesWithUnrequestedBlocks.clear(pieceIndex);
            partiallyMissingPieces.clear(pieceIndex);
            partiallyMissingPiecesWithUnrequestedBlocks.clear(pieceIndex);
            completePieces.set(pieceIndex);
            verifiedPieces.set(pieceIndex);
        }

        public synchronized void setPieceMissing(int pieceIndex) {
            verifiedPieces.clear(pieceIndex);
            completePieces.clear(pieceIndex);
            pieceIndexToAvailableBlocks.get(pieceIndex).clear();
            partiallyMissingPieces.clear(pieceIndex);
            partiallyMissingPiecesWithUnrequestedBlocks.clear(pieceIndex);
            completelyMissingPieces.set(pieceIndex);

            if (hasUnavailableAndUnrequestedBlocks(pieceIndex)) {
                completelyMissingPiecesWithUnrequestedBlocks.set(pieceIndex);
            } else {
                completelyMissingPiecesWithUnrequestedBlocks.clear(pieceIndex);
            }
        }

        public synchronized boolean isPieceComplete(int piece) {
            return completePieces.get(piece);
        }

        public synchronized boolean isAllPiecesVerified() {
            return verifiedPieces.cardinality() == getNumPieces();
        }

        public synchronized BitSet getVerifiedPieces() {
            return (BitSet) verifiedPieces.clone();
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
            unavailableAndUnrequestedBlocks.set(0, getNumBlocks(piece));
            unavailableAndUnrequestedBlocks.andNot(availableBlocks);
            unavailableAndUnrequestedBlocks.andNot(requestedBlocks);
            return unavailableAndUnrequestedBlocks;
        }

        public boolean isPieceVerified(int piece) {
            return verifiedPieces.get(piece);
        }

        private boolean isPieceCompletelyMissing(int piece) {
            return completelyMissingPieces.get(piece);
        }

        private boolean isPiecePartiallyMissing(int piece) {
            return partiallyMissingPieces.get(piece);
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
            unavailableBlocks.set(0, getNumBlocks(piece));
            unavailableBlocks.andNot(getAvailableBlocks(piece));
            return unavailableBlocks;
        }

        private BitSet getAvailableBlocks(int piece) {
            return pieceIndexToAvailableBlocks.get(piece);
        }

        private BitSet getUnrequestedBlocks(int piece) {
            BitSet unrequestedBlocks = new BitSet();
            unrequestedBlocks.set(0, getNumBlocks(piece));
            unrequestedBlocks.andNot(getRequestedBlocks(piece));
            return unrequestedBlocks;
        }

        private BitSet getRequestedBlocks(int piece) {
            return pieceIndexToRequestedBlocks.get(piece);
        }

        private boolean hasUnrequestedBlocks(int piece) {
            return pieceIndexToRequestedBlocks.get(piece).cardinality() < getNumBlocks(piece);
        }

        private boolean hasUnavailableBlocks(int piece) {
            return pieceIndexToAvailableBlocks.get(piece).cardinality() < getNumBlocks(piece);
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
            return availableBlocks.cardinality() == getNumBlocks(piece);
        }
    }
}
