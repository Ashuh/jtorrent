package jtorrent.domain.model.torrent;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.model.tracker.Tracker;
import jtorrent.domain.util.RangeList;
import jtorrent.domain.util.Sha1Hash;

public class Torrent {

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
    private final AtomicInteger uploaded = new AtomicInteger(0);
    private final Set<Peer> peers = new HashSet<>();

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

    public Sha1Hash getInfoHash() {
        return infoHash;
    }

    public long getDownloaded() {
        return downloaded.get();
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

    public long getRemainingBytes() {
        return getTotalSize() - getVerifiedBytes();
    }

    public int getNumPieces() {
        return pieceHashes.size();
    }

    public int getUploaded() {
        return uploaded.get();
    }

    public RangeList getFileByteRanges() {
        return fileByteRanges;
    }

    public long getPieceOffset(int index) {
        return (long) getPieceSize() * index;
    }

    public void incrementDownloaded(int amount) {
        downloaded.addAndGet(amount);
    }

    public void incrementUploaded(int amount) {
        uploaded.addAndGet(amount);
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

    public void addPeer(Peer peer) {
        peers.add(peer);
    }

    public boolean hasPeer(Peer peer) {
        return peers.contains(peer);
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
                + ", pieceHashes=" + pieceHashes
                + ", name='" + name + '\''
                + ", files=" + files
                + ", infoHash=" + infoHash
                + '}';
    }

    private class PieceTracker {

        private final HashMap<Integer, Map<Integer, Block.Status>> pieceIndexToBlockIndexToBlockStatus =
                new HashMap<>();

        private final BitSet completePieces = new BitSet();

        private final BitSet verifiedPieces = new BitSet();

        public PieceTracker() {
            IntStream.range(0, getNumPieces())
                    .forEach(i -> pieceIndexToBlockIndexToBlockStatus.put(i, initializeBlockIndexToBlockStatus(i)));
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
            completePieces.clear(pieceIndex);
        }

        public void setBlockIndexRequested(int pieceIndex, int blockIndex) {
            Map<Integer, Block.Status> blockIndexToBlockStatus = pieceIndexToBlockIndexToBlockStatus.get(pieceIndex);
            blockIndexToBlockStatus.put(blockIndex, Block.Status.REQUESTED);
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
            return pieceIndexToBlockIndexToBlockStatus.entrySet().stream()
                    .filter(entry -> entry.getValue().values().stream()
                            .anyMatch(status -> status == Block.Status.MISSING))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        public List<Integer> getCompletelyMissingPieceIndices() {
            return pieceIndexToBlockIndexToBlockStatus.entrySet().stream()
                    .filter(entry -> entry.getValue().values().stream()
                            .allMatch(status -> status == Block.Status.MISSING))
                    .map(Map.Entry::getKey)
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
