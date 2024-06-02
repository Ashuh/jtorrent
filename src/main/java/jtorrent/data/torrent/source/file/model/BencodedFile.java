package jtorrent.data.torrent.source.file.model;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jtorrent.data.torrent.source.file.model.util.MapUtil;
import jtorrent.domain.common.util.bencode.BencodedObject;
import jtorrent.domain.torrent.model.FileMetadata;

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
        int length = MapUtil.getValueAsLong(map, KEY_LENGTH).orElseThrow().intValue();
        List<ByteBuffer> pathRaw = MapUtil.getValueAsList(map, KEY_PATH);
        List<String> path = pathRaw.stream()
                .map(buffer -> new String(buffer.array()))
                .collect(Collectors.toList());
        return new BencodedFile(length, path);
    }

    public static BencodedFile fromPath(Path relativePath, long length) {
        List<String> pathComponents = getPathComponents(relativePath);
        return new BencodedFile(length, pathComponents);
    }

    public static BencodedFile fromDomain(FileMetadata fileMetadata) {
        return new BencodedFile(fileMetadata.size(), getPathComponents(fileMetadata.path()));
    }

    private static List<String> getPathComponents(Path path) {
        List<String> pathComponents = new LinkedList<>();
        Path current = path;
        while (current != null) {
            pathComponents.add(0, current.getFileName().toString());
            current = current.getParent();
        }
        return pathComponents;
    }

    public long getLength() {
        return length;
    }

    public List<String> getPath() {
        return path;
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
