package jtorrent.domain.torrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import jtorrent.domain.common.util.Sha1Hash;

public class MultiFileInfo extends FileInfo {

    private final String directory;

    public MultiFileInfo(String directory, List<FileMetadata> fileMetaData, int pieceSize,
            List<Sha1Hash> pieceHashes) {
        super(fileMetaData, pieceSize, pieceHashes);
        this.directory = requireNonNull(directory);
    }

    @Override
    public Path getFileRoot() {
        return Path.of(directory);
    }

    @Override
    public String getName() {
        return directory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), directory);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MultiFileInfo that = (MultiFileInfo) o;
        return Objects.equals(directory, that.directory);
    }

    @Override
    public String toString() {
        return "MultiFileInfo{"
                + "directory='" + directory + '\''
                + ", fileWithPieceInfos=" + fileMetaData
                + ", pieceHashes=" + pieceHashes
                + ", pieceSize=" + pieceSize
                + '}';
    }
}
