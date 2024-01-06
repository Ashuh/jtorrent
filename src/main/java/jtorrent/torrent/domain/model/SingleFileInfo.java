package jtorrent.torrent.domain.model;

import java.util.List;
import java.util.Optional;

import jtorrent.common.domain.util.Sha1Hash;

public class SingleFileInfo extends FileInfo {

    protected SingleFileInfo(List<FileWithPieceInfo> fileWithPieceInfos, int pieceSize, long totalFileSize,
            List<Sha1Hash> pieceHashes) {
        super(fileWithPieceInfos, pieceSize, totalFileSize, pieceHashes);
    }

    public static SingleFileInfo build(File file, int pieceSize, List<Sha1Hash> pieceHashes) {
        List<FileWithPieceInfo> fileWithPieceInfos = build(List.of(file), pieceSize);
        return new SingleFileInfo(fileWithPieceInfos, pieceSize, file.getSize(), pieceHashes);
    }

    @Override
    public Optional<String> getDirectory() {
        return Optional.empty();
    }

    @Override
    public boolean isSingleFile() {
        return true;
    }
}
