package jtorrent.domain.torrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import jtorrent.domain.common.util.Sha1Hash;

public class MultiFileInfo extends FileInfo {

    private final String directory;

    public MultiFileInfo(String directory, List<FileWithPieceInfo> fileWithPieceInfos, int pieceSize,
            long totalFileSize, List<Sha1Hash> pieceHashes) {
        super(fileWithPieceInfos, pieceSize, totalFileSize, pieceHashes);
        this.directory = requireNonNull(directory);
    }

    public static MultiFileInfo build(String directory, List<File> files, int pieceSize, List<Sha1Hash> pieceHashes) {
        List<FileWithPieceInfo> fileWithPieceInfos = build(files, pieceSize);
        long totalFileSize = files.stream()
                .mapToLong(File::getSize)
                .sum();
        return new MultiFileInfo(directory, fileWithPieceInfos, pieceSize, totalFileSize, pieceHashes);
    }

    @Override
    public boolean isSingleFile() {
        return false;
    }

    @Override
    public Path getFileRoot() {
        return getSaveDirectory().resolve(directory);
    }

    @Override
    public Path getSaveAsPath() {
        return getFileRoot();
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
                + ", fileWithPieceInfos=" + fileWithPieceInfos
                + ", pieceHashes=" + pieceHashes
                + ", pieceSize=" + pieceSize
                + ", totalFileSize=" + totalFileSize
                + ", saveDirectory=" + saveDirectory
                + '}';
    }
}
