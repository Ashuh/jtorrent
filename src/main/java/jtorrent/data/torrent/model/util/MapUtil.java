package jtorrent.data.torrent.model.util;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MapUtil {

    public static Optional<String> getValueAsString(Map<String, Object> map, String key) {
        return getValue(map, key).map(o -> (ByteBuffer) o)
                .map(ByteBuffer::array)
                .map(String::new);
    }

    private static Optional<Object> getValue(Map<String, Object> map, String key) {
        return Optional.ofNullable(map.get(key));
    }

    public static Optional<Long> getValueAsLong(Map<String, Object> map, String key) {
        return getValue(map, key).map(o -> (Long) o);
    }

    public static Optional<byte[]> getValueAsByteArray(Map<String, Object> map, String key) {
        return getValue(map, key).map(o -> (ByteBuffer) o)
                .map(ByteBuffer::array);
    }

    public static <K, V> Map<K, V> getValueAsMap(Map<String, Object> map, String key) {
        return getValue(map, key).map(o -> (Map<K, V>) o).orElseGet(Collections::emptyMap);
    }

    public static <T> List<T> getValueAsList(Map<String, Object> map, String key) {
        return getValue(map, key).map(o -> (List<T>) o).orElseGet(Collections::emptyList);
    }
}
