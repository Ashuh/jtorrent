package jtorrent.domain.model.torrent;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Torrent {

    private final List<URI> trackers;
    private final LocalDateTime creationDate;
    private final String comment;
    private final String createdBy;
    private final int pieceSize;
    private final List<Sha1Hash> pieceHashes;
    private final String name;
    private final List<File> files;
    private final Sha1Hash infoHash;

    public Torrent(List<URI> trackers, LocalDateTime creationDate, String comment, String createdBy, int pieceSize,
            List<Sha1Hash> pieceHashes, String name, List<File> files, Sha1Hash infoHash) {
        this.trackers = requireNonNull(trackers);
        this.creationDate = requireNonNull(creationDate);
        this.comment = requireNonNull(comment);
        this.createdBy = requireNonNull(createdBy);
        this.pieceSize = pieceSize;
        this.pieceHashes = requireNonNull(pieceHashes);
        this.name = requireNonNull(name);
        this.files = requireNonNull(files);
        this.infoHash = requireNonNull(infoHash);
    }

    public List<URI> getTrackers() {
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
            return getTotalSize() % pieceSize;
        }
        return pieceSize;
    }

    public int getNumPieces() {
        return pieceHashes.size();
    }

    public int getTotalSize() {
        return files.stream()
                .map(File::getSize)
                .mapToInt(Integer::intValue)
                .sum();
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
        return "Torrent{" +
                "trackers=" + trackers +
                ", creationDate=" + creationDate +
                ", comment='" + comment + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", pieceSize=" + pieceSize +
                ", pieceHashes=" + pieceHashes +
                ", name='" + name + '\'' +
                ", files=" + files +
                ", infoHash=" + infoHash +
                '}';
    }
}
