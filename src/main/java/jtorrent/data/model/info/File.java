package jtorrent.data.model.info;

import static jtorrent.data.util.MapUtil.getValueAsList;
import static jtorrent.data.util.MapUtil.getValueAsLong;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jtorrent.data.model.BencodedObject;

public class File extends BencodedObject {

    private static final String KEY_LENGTH = "length";
    private static final String KEY_PATH = "path";

    private final int length;
    private final List<String> path;

    public File(int length, List<String> path) {
        this.length = length;
        this.path = path;
    }

    public static File fromMap(Map<String, Object> map) {
        int length = getValueAsLong(map, KEY_LENGTH).orElseThrow().intValue();
        List<ByteBuffer> pathRaw = getValueAsList(map, KEY_PATH);
        List<String> path = pathRaw.stream()
                .map(buffer -> new String(buffer.array()))
                .collect(Collectors.toList());
        return new File(length, path);
    }

    public int getLength() {
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
        File file = (File) o;
        return length == file.length && Objects.equals(path, file.path);
    }

    @Override
    public String toString() {
        return "File{" +
                "length=" + length +
                ", path=" + path +
                '}';
    }
}
