package jtorrent.torrent.data.model;

import static jtorrent.torrent.data.model.util.MapUtil.getValueAsList;
import static jtorrent.torrent.data.model.util.MapUtil.getValueAsLong;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jtorrent.common.domain.util.bencode.BencodedObject;
import jtorrent.torrent.domain.model.File;

public class BencodedFile extends BencodedObject {

    public static final String KEY_LENGTH = "length";
    public static final String KEY_PATH = "path";

    private final long length;
    private final List<String> path;

    public BencodedFile(long length, List<String> path) {
        this.length = length;
        this.path = path;
    }

    public static BencodedFile fromMap(Map<String, Object> map) {
        int length = getValueAsLong(map, KEY_LENGTH).orElseThrow().intValue();
        List<ByteBuffer> pathRaw = getValueAsList(map, KEY_PATH);
        List<String> path = pathRaw.stream()
                .map(buffer -> new String(buffer.array()))
                .collect(Collectors.toList());
        return new BencodedFile(length, path);
    }

    public long getLength() {
        return length;
    }

    public List<String> getPath() {
        return path;
    }

    public File toDomain() {
        List<String> sanitizedPath = path.stream()
                .map(part -> part.replaceAll("[\\\\/:*?\"<>|]", "_"))
                .collect(Collectors.toList());
        return new File(length, Path.of(String.join("/", sanitizedPath)));
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
                KEY_LENGTH, length,
                KEY_PATH, path
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(length, path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BencodedFile file = (BencodedFile) o;
        return length == file.length && Objects.equals(path, file.path);
    }

    @Override
    public String toString() {
        return "File{"
                + "length=" + length
                + ", path=" + path
                + '}';
    }
}
