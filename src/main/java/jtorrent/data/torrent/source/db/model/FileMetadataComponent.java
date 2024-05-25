package jtorrent.data.torrent.source.db.model;

import java.nio.file.Path;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jtorrent.domain.torrent.model.FileMetadata;

@Embeddable
public class FileMetadataComponent {

    @Column(nullable = false)
    private final long size;

    @Column(nullable = false)
    private final String path;

    @Column(nullable = false)
    private final int firstPiece;

    @Column(nullable = false)
    private final int firstPieceStart;

    @Column(nullable = false)
    private final int lastPiece;

    @Column(nullable = false)
    private final int lastPieceEnd;

    @Column(nullable = false)
    private final long start;

    protected FileMetadataComponent() {
        this(0, "", 0, 0, 0, 0, 0);
    }

    public FileMetadataComponent(long size, String path, int firstPiece, int firstPieceStart, int lastPiece,
            int lastPieceEnd, long start) {
        this.size = size;
        this.path = path;
        this.firstPiece = firstPiece;
        this.firstPieceStart = firstPieceStart;
        this.lastPiece = lastPiece;
        this.lastPieceEnd = lastPieceEnd;
        this.start = start;
    }

    public static FileMetadataComponent fromDomain(FileMetadata fileMetadata) {
        return new FileMetadataComponent(
                fileMetadata.size(),
                fileMetadata.path().toString(),
                fileMetadata.firstPiece(),
                fileMetadata.firstPieceStart(),
                fileMetadata.lastPiece(),
                fileMetadata.lastPieceEnd(),
                fileMetadata.start()
        );
    }

    public FileMetadata toDomain() {
        return new FileMetadata(
                Path.of(path),
                start,
                size,
                firstPiece,
                firstPieceStart,
                lastPiece,
                lastPieceEnd
        );
    }

    public long getSize() {
        return size;
    }

    public String getPath() {
        return path;
    }

    public int getFirstPiece() {
        return firstPiece;
    }

    public int getFirstPieceStart() {
        return firstPieceStart;
    }

    public int getLastPiece() {
        return lastPiece;
    }

    public int getLastPieceEnd() {
        return lastPieceEnd;
    }

    public long getStart() {
        return start;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(size);
        result = 31 * result + path.hashCode();
        result = 31 * result + firstPiece;
        result = 31 * result + firstPieceStart;
        result = 31 * result + lastPiece;
        result = 31 * result + lastPieceEnd;
        result = 31 * result + Long.hashCode(start);
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

        FileMetadataComponent that = (FileMetadataComponent) o;
        return size == that.size
                && firstPiece == that.firstPiece
                && firstPieceStart == that.firstPieceStart
                && lastPiece == that.lastPiece
                && lastPieceEnd == that.lastPieceEnd
                && start == that.start
                && path.equals(that.path);
    }

    @Override
    public String toString() {
        return "FileMetadataComponent{"
                + "size=" + size
                + ", path='" + path + '\''
                + ", firstPiece=" + firstPiece
                + ", firstPieceStart=" + firstPieceStart
                + ", lastPiece=" + lastPiece
                + ", lastPieceEnd=" + lastPieceEnd
                + ", start=" + start
                + '}';
    }
}
