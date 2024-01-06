package jtorrent.torrent.domain.model;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.util.List;
import java.util.Optional;

import jtorrent.common.domain.util.Sha1Hash;

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

    public Optional<String> getDirectory() {
        return directory.describeConstable();
    }

    @Override
    public boolean isSingleFile() {
        return false;
    }
}
