package jtorrent.data.torrent.model;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jtorrent.data.torrent.model.util.MapUtil;
import jtorrent.domain.torrent.model.File;
import jtorrent.domain.torrent.model.FileInfo;
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

    @Override
    public List<BencodedFile> getFiles() {
        return List.of(new BencodedFile(length, List.of(name)));
    }

    @Override
    public FileInfo toDomain() {
        File domainFile = new File(length, Path.of(name));
        return SingleFileInfo.build(domainFile, pieceLength, getDomainPieceHashes());
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