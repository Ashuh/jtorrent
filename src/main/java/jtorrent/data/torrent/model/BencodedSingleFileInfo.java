package jtorrent.data.torrent.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jtorrent.data.torrent.model.util.MapUtil;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.SingleFileInfo;

public class BencodedSingleFileInfo extends BencodedInfo {

    private final long length;

    public BencodedSingleFileInfo(int pieceLength, byte[] pieces, String name, long length) {
        super(pieceLength, pieces, name);
        this.length = length;
    }

    public static BencodedSingleFileInfo fromMap(Map<String, Object> map) {
        int pieceLength = MapUtil.getValueAsLong(map, KEY_PIECE_LENGTH).orElseThrow().intValue();
        byte[] pieces = MapUtil.getValueAsByteArray(map, KEY_PIECES).orElseThrow();
        String name = MapUtil.getValueAsString(map, KEY_NAME).orElseThrow();
        long length = MapUtil.getValueAsLong(map, KEY_LENGTH).orElseThrow();

        return new BencodedSingleFileInfo(pieceLength, pieces, name, length);
    }

    public static BencodedSingleFileInfo fromPath(Path source, int pieceSize)
            throws IOException {
        if (Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must be a file");
        }

        byte[] hashes = computeHashes(Files.newInputStream(source), pieceSize);
        String fileName = source.getFileName().toString();
        long length = Files.size(source);
        return new BencodedSingleFileInfo(pieceSize, hashes, fileName, length);
    }

    public static BencodedSingleFileInfo fromDomain(SingleFileInfo fileInfo) {
        byte[] pieces = concatHashes(fileInfo.getPieceHashes());
        String name = fileInfo.getFileMetaData().get(0).path().getFileName().toString();
        long length = fileInfo.getFileMetaData().get(0).size();
        return new BencodedSingleFileInfo(fileInfo.getPieceSize(), pieces, name, length);
    }

    @Override
    public List<BencodedFile> getFiles() {
        return List.of(new BencodedFile(length, List.of(name)));
    }

    @Override
    public long getTotalSize() {
        return length;
    }

    @Override
    public FileInfo toDomain() {
        FileMetadata fileMetaData = buildFileMetaData();
        return new SingleFileInfo(fileMetaData, pieceLength, getDomainPieceHashes());
    }

    private FileMetadata buildFileMetaData() {
        Path filePath = Path.of(name);
        int lastPiece = getNumPieces() - 1;
        long fileEnd = length - 1;
        int lastPieceEnd = (int) (fileEnd % pieceLength);

        return new FileMetadata(length, filePath, 0, 0,
                lastPiece, lastPieceEnd, 0, fileEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), length);
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
        BencodedSingleFileInfo that = (BencodedSingleFileInfo) o;
        return length == that.length;
    }

    public long getLength() {
        return length;
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
                KEY_PIECE_LENGTH, pieceLength,
                KEY_PIECES, ByteBuffer.wrap(pieces),
                KEY_NAME, name,
                KEY_LENGTH, length
        );
    }

    @Override
    public String toString() {
        return "SingleFIleInfo{"
                + "pieceLength=" + pieceLength
                + ", pieces=" + Arrays.toString(pieces)
                + ", name='" + name + '\''
                + ", length=" + length
                + '}';
    }
}
