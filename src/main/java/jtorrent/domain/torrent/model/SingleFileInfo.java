package jtorrent.domain.torrent.model;

import java.nio.file.Path;
import java.util.List;

import jtorrent.domain.common.util.Sha1Hash;

public class SingleFileInfo extends FileInfo {

    public SingleFileInfo(FileMetadata fileMetaData, int pieceSize, List<Sha1Hash> pieceHashes) {
        super(List.of(fileMetaData), pieceSize, pieceHashes);
    }

    @Override
    public Path getFileRoot() {
        return Path.of("");
    }

    @Override
    public String getName() {
        return getFileMetaData().get(0).path().getFileName().toString();
    }

    @Override
    public String toString() {
        return "SingleFileInfo{"
                + "fileWithPieceInfos=" + fileMetaData
                + ", pieceHashes=" + pieceHashes
                + ", pieceSize=" + pieceSize
                + "} ";
    }
}
