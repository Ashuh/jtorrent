package jtorrent.torrent.domain.model;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import jtorrent.common.domain.model.Block;
import jtorrent.common.domain.util.RangeList;
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
    private final Sha1Hash infoHash;
    private final RangeList fileByteRanges;
    private final PieceTracker pieceTracker;
    private final AtomicInteger downloaded = new AtomicInteger(0);
    private final BehaviorSubject<Integer> downloadedSubject = BehaviorSubject.createDefault(0);
    private final AtomicInteger uploaded = new AtomicInteger(0);
    private final BehaviorSubject<Integer> uploadedSubject = BehaviorSubject.createDefault(0);
    private final MutableRxObservableSet<Peer> peers = new MutableRxObservableSet<>(new HashSet<>());
    private final CombinedDoubleSumObservable downloadRateObservable = new CombinedDoubleSumObservable();
    private final AtomicLong verifiedBytes = new AtomicLong(0);
    private final BehaviorSubject<Long> verifiedBytesSubject = BehaviorSubject.createDefault(0L);
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
        this.infoHash = requireNonNull(infoHash);
        this.fileByteRanges = RangeList.fromRangeSizes(0, files.stream()
                .map(File::getSize)
                .collect(Collectors.toList()));
        this.pieceTracker = new PieceTracker();
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

    public String getName() {
        return name;
    }

    public List<File> getFiles() {
        return files;
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
        return pieceTracker.getVerifiedPieceIndices()
                .map(this::getPieceSize)
                .sum();
    }

    public int getNumPieces() {
        return pieceHashes.size();
    }

    public RangeList getFileByteRanges() {
        return fileByteRanges;
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

    public void setBlockMissing(int pieceIndex, int blockIndex) {
        pieceTracker.setBlockMissing(pieceIndex, blockIndex);
    }

    public void setPieceMissing(int pieceIndex) {
        IntStream.range(0, getNumBlocks(pieceIndex))
                .forEach(i -> setBlockMissing(pieceIndex, i));
    }

    public List<Integer> getCompletelyMissingPieceIndices() {
        return pieceTracker.getCompletelyMissingPieceIndices();
    }

    public List<Integer> getPartiallyMissingPieceIndices() {
        return pieceTracker.getPartiallyMissingPieceIndices();
    }

    public void setPieceVerified(int pieceIndex) {
        pieceTracker.setPieceVerified(pieceIndex);
        verifiedBytes.getAndAdd(getPieceSize(pieceIndex));
        verifiedBytesSubject.onNext(verifiedBytes.get());
    }

    public boolean isPieceComplete(int pieceIndex) {
        return pieceTracker.isPieceComplete(pieceIndex);
    }

    public boolean isAllPiecesVerified() {
        return pieceTracker.isAllPiecesVerified();
    }

    public List<Integer> getmissingBlockIndices(int pieceIndex) {
        return pieceTracker.getMissingBlockIndices(pieceIndex);
    }

    public void setBlockRequested(int pieceIndex, int blockIndex) {
        pieceTracker.setBlockIndexRequested(pieceIndex, blockIndex);
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

    public Observable<Long> getVerifiedBytesObservable() {
        return verifiedBytesSubject;
    }

    public Observable<Integer> getUploadedObservable() {
        return uploadedSubject;
    }

    public RxObservableSet<Peer> getPeersObservable() {
        return peers;
    }

    public void addPeer(Peer peer) {
        peers.add(peer);
        downloadRateObservable.addSource(peer.getDownloadRateObservable());
    }

    public void removePeer(Peer peer) {
        peers.remove(peer);
        downloadRateObservable.removeSource(peer.getDownloadRateObservable());
    }

    public void clearPeers() {
        peers.clear();
        downloadRateObservable.clearSources();
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

        private final HashMap<Integer, Map<Integer, Block.Status>> pieceIndexToBlockIndexToBlockStatus =
                new HashMap<>();

        private final BitSet partiallyMissingPieces = new BitSet();
        private final BitSet completelyMissingPieces = new BitSet();
        private final BitSet completePieces = new BitSet();
        private final BitSet verifiedPieces = new BitSet();

        public PieceTracker() {
            IntStream.range(0, getNumPieces())
                    .forEach(i -> {
                        pieceIndexToBlockIndexToBlockStatus.put(i, initializeBlockIndexToBlockStatus(i));
                        completelyMissingPieces.set(i);
                    });
        }

        private Map<Integer, Block.Status> initializeBlockIndexToBlockStatus(int pieceIndex) {
            return IntStream.range(0, getNumBlocks(pieceIndex))
                    .boxed()
                    .collect(Collectors.toMap(Function.identity(), i -> Block.Status.MISSING, (a, b) -> b));
        }

        public void setBlockMissing(int pieceIndex, int blockIndex) {
            if (isPieceComplete(pieceIndex)) {
                completePieces.clear(pieceIndex);
            }

            Map<Integer, Block.Status> blockIndexToBlockStatus = pieceIndexToBlockIndexToBlockStatus.get(pieceIndex);
            blockIndexToBlockStatus.put(blockIndex, Block.Status.MISSING);
            if (blockIndexToBlockStatus.values().stream()
                    .allMatch(status -> status == Block.Status.MISSING)) {
                completelyMissingPieces.set(pieceIndex);
            } else {
                partiallyMissingPieces.set(pieceIndex);
            }
        }

        public void setBlockIndexRequested(int pieceIndex, int blockIndex) {
            Map<Integer, Block.Status> blockIndexToBlockStatus = pieceIndexToBlockIndexToBlockStatus.get(pieceIndex);
            blockIndexToBlockStatus.put(blockIndex, Block.Status.REQUESTED);

            if (completelyMissingPieces.get(pieceIndex)) {
                completelyMissingPieces.clear(pieceIndex);
                partiallyMissingPieces.set(pieceIndex);
            } else if (partiallyMissingPieces.get(pieceIndex)
                    && (blockIndexToBlockStatus.values().stream()
                    .noneMatch(status -> status == Block.Status.MISSING))) {
                partiallyMissingPieces.clear(pieceIndex);
            }
        }

        public void setBlockReceived(int pieceIndex, int blockIndex) {
            if (isPieceComplete(pieceIndex)) {
                return;
            }

            Map<Integer, Block.Status> blockIndexToBlockStatus = pieceIndexToBlockIndexToBlockStatus.get(pieceIndex);
            blockIndexToBlockStatus.put(blockIndex, Block.Status.RECEIVED);

            boolean isAllBlocksReceived = blockIndexToBlockStatus.values().stream()
                    .allMatch(status -> status == Block.Status.RECEIVED);

            if (isAllBlocksReceived) {
                completePieces.set(pieceIndex);
                partiallyMissingPieces.clear(pieceIndex);
            }
        }

        public void setPieceVerified(int pieceIndex) {
            verifiedPieces.set(pieceIndex);
        }

        public boolean isPieceComplete(int pieceIndex) {
            return completePieces.get(pieceIndex);
        }

        public boolean isAllPiecesVerified() {
            return verifiedPieces.cardinality() == getNumPieces();
        }

        public IntStream getVerifiedPieceIndices() {
            return verifiedPieces.stream();
        }

        public List<Integer> getPartiallyMissingPieceIndices() {
            return partiallyMissingPieces.stream()
                    .boxed()
                    .collect(Collectors.toList());
        }

        public List<Integer> getCompletelyMissingPieceIndices() {
            return completelyMissingPieces.stream()
                    .boxed()
                    .collect(Collectors.toList());
        }

        public List<Integer> getMissingBlockIndices(int pieceIndex) {
            return pieceIndexToBlockIndexToBlockStatus.get(pieceIndex).entrySet().stream()
                    .filter(entry -> entry.getValue() == Block.Status.MISSING)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
    }
}
