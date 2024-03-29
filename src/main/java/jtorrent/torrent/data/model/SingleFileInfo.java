package jtorrent.torrent.data.model;

import static jtorrent.torrent.data.model.util.MapUtil.getValueAsByteArray;
import static jtorrent.torrent.data.model.util.MapUtil.getValueAsLong;
import static jtorrent.torrent.data.model.util.MapUtil.getValueAsString;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SingleFileInfo extends BencodedInfo {

    private final long length;

    public SingleFileInfo(int pieceLength, byte[] pieces, String name, long length) {
        super(pieceLength, pieces, name);
        this.length = length;
    }

    public static SingleFileInfo fromMap(Map<String, Object> map) {
        int pieceLength = getValueAsLong(map, KEY_PIECE_LENGTH).orElseThrow().intValue();
        byte[] pieces = getValueAsByteArray(map, KEY_PIECES).orElseThrow();
        String name = getValueAsString(map, KEY_NAME).orElseThrow();
        long length = getValueAsLong(map, KEY_LENGTH).orElseThrow();

        return new SingleFileInfo(pieceLength, pieces, name, length);
    }

    @Override
    public List<BencodedFile> getFiles() {
        return List.of(new BencodedFile(length, List.of(name)));
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
        SingleFileInfo that = (SingleFileInfo) o;
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
