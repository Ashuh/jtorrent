package jtorrent.domain.torrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.rx.CombinedDoubleSumObservable;
import jtorrent.domain.common.util.rx.MutableRxObservableSet;
import jtorrent.domain.common.util.rx.RxObservableSet;
import jtorrent.domain.peer.model.Peer;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.tracker.handler.TrackerHandler;
import jtorrent.domain.tracker.model.Tracker;

public class Torrent implements TrackerHandler.TorrentProgressProvider {

    private static final int BLOCK_SIZE = 16384;

    private final Set<Tracker> trackers;
    private final LocalDateTime creationDate;
    private final String comment;
    private final String createdBy;
    private String name;
    private final BehaviorSubject<String> nameSubject = BehaviorSubject.createDefault("");
    private final FileInfo fileInfo;
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
            String name, FileInfo fileInfo, Sha1Hash infoHash) {
        this.trackers = requireNonNull(trackers);
        this.creationDate = requireNonNull(creationDate);
        this.comment = requireNonNull(comment);
        this.createdBy = requireNonNull(createdBy);
        this.name = requireNonNull(name);
        this.fileInfo = requireNonNull(fileInfo);
        this.infoHash = requireNonNull(infoHash);
        this.pieceTracker = new PieceTracker();
    }

    public Path getSaveDirectory() {
        return fileInfo.getSaveDirectory();
    }

    public void setSaveDirectory(Path saveDirectory) {
        fileInfo.setSaveDirectory(saveDirectory);
    }

    /**
     * Gets the root save directory for this torrent.
     * The paths of the files in the torrent are relative to the root save directory.
     * If the torrent is a single-file torrent, the root save directory is the save directory.
     * If the torrent is a multi-file torrent, the root save directory is a subdirectory of the save directory.
     *
     * @return the root save directory for this torrent
     */
    public Path getRootSaveDirectory() {
        return fileInfo.getFileRoot();
    }

    /**
     * Checks whether this torrent is a single-file torrent or a multi-file torrent.
     * <p>
     * Note: A multi-file torrent does not necessarily have multiple files.
     * The difference between the two is that a multi-file torrent is a torrent created from a directory,
     * containing one or more files, whereas a single-file torrent is a torrent created from a single file.
     * The contents of a multi-file torrent are stored in a subdirectory of the save directory,
     * whereas the contents of a single-file torrent are stored in the save directory itself.
     *
     * @return true if this torrent is a single-file torrent, false if it is a multi-file torrent
     */
    public boolean isSingleFileTorrent() {
        return fileInfo.isSingleFile();
    }

    public Path getSaveAsPath() {
        return fileInfo.getSaveAsPath();
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
        return fileInfo.getPieceSize();
    }

    public int getPieceSize(int pieceIndex) {
        if (pieceIndex == getNumPieces() - 1) {
            int remainder = (int) (getTotalSize() % getPieceSize());
            return remainder == 0 ? getPieceSize() : remainder;
        }
        return getPieceSize();
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
        return fileInfo.getPieceHashes();
    }

    public Sha1Hash getPieceHash(int piece) {
        return fileInfo.getPieceHash(piece);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<File> getFiles() {
        return fileInfo.getFiles();
    }

    public List<FileWithPieceInfo> getFileWithInfosInRange(long start, long end) {
        return fileInfo.getInRange(start, end);
    }

    public List<FileWithPieceInfo> getFilesWithInfo() {
        return fileInfo.getFileWithInfos();
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
        return fileInfo.getTotalFileSize();
    }

    private long getVerifiedBytes() {
        return pieceTracker.getVerifiedPieces()
                .stream()
                .mapToLong(this::getPieceSize)
                .sum();
    }

    public int getNumPieces() {
        return fileInfo.getNumPieces();
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

            long pieceStart = getPieceOffset(pieceIndex);
            long pieceEnd = pieceStart + getPieceSize(pieceIndex) - 1;
            List<FileWithPieceInfo> filesInRange = fileInfo.getInRange(pieceStart, pieceEnd);

            for (FileWithPieceInfo fileWithPieceInfo : filesInRange) {
                FilePieceInfo filePieceInfo = fileWithPieceInfo.filePieceInfo();
                filePieceInfo.setPieceNotVerified(pieceIndex - filePieceInfo.firstPiece());
                long pieceBytesWithinFile = getPieceBytesInFile(pieceIndex, filePieceInfo);
                filePieceInfo.incrementVerifiedBytes(-pieceBytesWithinFile);
            }
        }
        pieceTracker.setPieceMissing(pieceIndex);
        verifiedPiecesSubject.onNext(pieceTracker.getVerifiedPieces());
    }

    private long getPieceBytesInFile(int piece, FilePieceInfo filePieceInfo) {
        long pieceStart = getPieceOffset(piece);
        long pieceEnd = pieceStart + getPieceSize(piece) - 1;
        long pieceStartWithinFile = Math.max(pieceStart, filePieceInfo.start());
        long pieceEndWithinFile = Math.min(pieceEnd, filePieceInfo.end());
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
        List<FileWithPieceInfo> filesInRange = fileInfo.getInRange(pieceStart, pieceEnd);

        for (FileWithPieceInfo fileWithPieceInfo : filesInRange) {
            FilePieceInfo filePieceInfo = fileWithPieceInfo.filePieceInfo();
            filePieceInfo.setPieceVerified(pieceIndex - filePieceInfo.firstPiece());
            long pieceBytesWithinFile = getPieceBytesInFile(pieceIndex, filePieceInfo);
            filePieceInfo.incrementVerifiedBytes(pieceBytesWithinFile);
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Torrent torrent = (Torrent) o;
        return Objects.equals(infoHash, torrent.infoHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(infoHash);
    }

    @Override
    public String toString() {
        return "Torrent{"
                + "trackers=" + trackers
                + ", creationDate=" + creationDate
                + ", comment='" + comment + '\''
                + ", createdBy='" + createdBy + '\''
                + ", name='" + name + '\''
                + ", fileInfo=" + fileInfo
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