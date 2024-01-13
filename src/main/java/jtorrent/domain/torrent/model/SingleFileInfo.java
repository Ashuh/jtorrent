package jtorrent.domain.torrent.model;

import java.nio.file.Path;
import java.util.List;

import jtorrent.domain.common.util.Sha1Hash;

public class SingleFileInfo extends FileInfo {

    protected SingleFileInfo(List<FileWithPieceInfo> fileWithPieceInfos, int pieceSize, long totalFileSize,
            List<Sha1Hash> pieceHashes) {
        super(fileWithPieceInfos, pieceSize, totalFileSize, pieceHashes);
    }

    public static SingleFileInfo build(File file, int pieceSize, List<Sha1Hash> pieceHashes) {
        if (file.getPath().getNameCount() != 1) {
            throw new IllegalArgumentException("File path must be a single file");
        }
        List<FileWithPieceInfo> fileWithPieceInfos = build(List.of(file), pieceSize);
        return new SingleFileInfo(fileWithPieceInfos, pieceSize, file.getSize(), pieceHashes);
    }

    @Override
    public boolean isSingleFile() {
        return true;
    }

    @Override
    public Path getFileRoot() {
        return getSaveDirectory();
    }

    @Override
    public Path getSaveAsPath() {
        return getFileRoot().resolve(getFiles().get(0).getPath());
    }

    @Override
    public String toString() {
        return "SingleFileInfo{"
                + "fileWithPieceInfos=" + fileWithPieceInfos
                + ", pieceHashes=" + pieceHashes
                + ", pieceSize=" + pieceSize
                + ", totalFileSize=" + totalFileSize
                + ", saveDirectory=" + saveDirectory
                + "} ";
    }
}
