package jtorrent.domain.model.torrent;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jtorrent.domain.model.tracker.Tracker;
import jtorrent.domain.util.RangeList;
import jtorrent.domain.util.Sha1Hash;

public class Torrent {

    private final List<Tracker> trackers;
    private final LocalDateTime creationDate;
    private final String comment;
    private final String createdBy;
    private final int pieceSize;
    private final List<Sha1Hash> pieceHashes;
    private final String name;
    private final List<File> files;
    private final Sha1Hash infoHash;
    private final RangeList fileByteRanges;
    private final PieceTracker pieceTracker = new PieceTracker();
    private final AtomicInteger downloaded = new AtomicInteger(0);
    private final AtomicInteger uploaded = new AtomicInteger(0);

    public Torrent(List<Tracker> trackers, LocalDateTime creationDate, String comment, String createdBy,
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
    }

    public List<Tracker> getTrackers() {
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
            return (int) (getTotalSize() % pieceSize);
        }
        return pieceSize;
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
                .map(this::getPieceSize)
                .sum();
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

    public void setDataReceived(int pieceIndex, int from, int to) {
        pieceTracker.setDataReceived(pieceIndex, from, to);
    }

    public void unsetDataReceived(int pieceIndex, int from, int to) {
        pieceTracker.unsetDataReceived(pieceIndex, from, to);
    }

    public void setPieceVerified(int pieceIndex) {
        pieceTracker.setPieceVerified(pieceIndex);
    }

    public boolean isPieceComplete(int pieceIndex) {
        return pieceTracker.isPieceComplete(pieceIndex);
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

        private final HashMap<Integer, BitSet> pieceIndexToAvailableBytes = new HashMap<>();

        private final BitSet availablePieces = new BitSet();

        private final BitSet verifiedPieces = new BitSet();

        public void setDataReceived(int pieceIndex, int from, int to) {
            if (isPieceComplete(pieceIndex)) {
                return;
            }

            BitSet availableBytes = pieceIndexToAvailableBytes.computeIfAbsent(pieceIndex, k -> new BitSet());
            availableBytes.set(from, to);

            if (availableBytes.cardinality() == getPieceSize(pieceIndex)) {
                availablePieces.set(pieceIndex);
                pieceIndexToAvailableBytes.remove(pieceIndex);
            }
        }

        public void unsetDataReceived(int pieceIndex, int from, int to) {
            BitSet availableBytes = pieceIndexToAvailableBytes.computeIfAbsent(pieceIndex, k -> new BitSet());

            if (availablePieces.get(pieceIndex)) {
                availablePieces.clear(pieceIndex);
                assert availableBytes.isEmpty();
                availableBytes.set(0, getPieceSize(pieceIndex));
            }

            availableBytes.clear(from, to);
        }

        public void setPieceVerified(int pieceIndex) {
            verifiedPieces.set(pieceIndex);
        }

        public boolean isPieceComplete(int pieceIndex) {
            return availablePieces.get(pieceIndex);
        }

        public IntStream getAvailablePieces() {
            return availablePieces.stream();
        }

        public IntStream getVerifiedPieces() {
            return verifiedPieces.stream();
        }
    }
}
