package jtorrent.data.torrent.model;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jtorrent.data.torrent.model.util.MapUtil;
import jtorrent.domain.torrent.model.File;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.MultiFileInfo;

public class BencodedMultiFileInfo extends BencodedInfo {

    private final List<BencodedFile> files;

    public BencodedMultiFileInfo(int pieceLength, byte[] pieces, String name, List<BencodedFile> files) {
        super(pieceLength, pieces, name);
        this.files = files;
    }

    public static BencodedMultiFileInfo fromMap(Map<String, Object> map) {
        int pieceLength = MapUtil.getValueAsLong(map, KEY_PIECE_LENGTH).orElseThrow().intValue();
        byte[] pieces = MapUtil.getValueAsByteArray(map, KEY_PIECES).orElseThrow();
        String name = MapUtil.getValueAsString(map, KEY_NAME).orElseThrow();
        List<Map<String, Object>> filesRaw = MapUtil.getValueAsList(map, KEY_FILES);
        List<BencodedFile> files = filesRaw.stream()
                .map(BencodedFile::fromMap)
                .collect(Collectors.toList());

        return new BencodedMultiFileInfo(pieceLength, pieces, name, files);
    }

    @Override
    public List<BencodedFile> getFiles() {
        return files;
    }

    @Override
    public FileInfo toDomain() {
        List<File> domainFiles = this.files.stream()
                .map(BencodedFile::toDomain)
                .toList();
        return MultiFileInfo.build(name, domainFiles, pieceLength, getDomainPieceHashes());
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
        BencodedMultiFileInfo that = (BencodedMultiFileInfo) o;
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
