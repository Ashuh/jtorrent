package jtorrent.torrent.data.model;

import static jtorrent.torrent.data.model.util.MapUtil.getValueAsByteArray;
import static jtorrent.torrent.data.model.util.MapUtil.getValueAsList;
import static jtorrent.torrent.data.model.util.MapUtil.getValueAsLong;
import static jtorrent.torrent.data.model.util.MapUtil.getValueAsString;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MultiFileInfo extends BencodedInfo {

    private final List<BencodedFile> files;

    public MultiFileInfo(int pieceLength, byte[] pieces, String name, List<BencodedFile> files) {
        super(pieceLength, pieces, name);
        this.files = files;
    }

    public static MultiFileInfo fromMap(Map<String, Object> map) {
        int pieceLength = getValueAsLong(map, KEY_PIECE_LENGTH).orElseThrow().intValue();
        byte[] pieces = getValueAsByteArray(map, KEY_PIECES).orElseThrow();
        String name = getValueAsString(map, KEY_NAME).orElseThrow();
        List<Map<String, Object>> filesRaw = getValueAsList(map, KEY_FILES);
        List<BencodedFile> files = filesRaw.stream()
                .map(BencodedFile::fromMap)
                .collect(Collectors.toList());

        return new MultiFileInfo(pieceLength, pieces, name, files);
    }

    @Override
    public List<BencodedFile> getFiles() {
        return files;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), files);
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
        return Objects.equals(files, that.files);
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
                KEY_PIECE_LENGTH, pieceLength,
                KEY_PIECES, ByteBuffer.wrap(pieces),
                KEY_NAME, name,
                KEY_FILES, files.stream().map(BencodedFile::toMap).collect(Collectors.toList())
        );
    }

    @Override
    public String toString() {
        return "MultiFileInfo{"
                + "pieceLength=" + pieceLength
                + ", pieces=" + Arrays.toString(pieces)
                + ", name='" + name + '\''
                + ", files=" + files
                + '}';
    }
}
