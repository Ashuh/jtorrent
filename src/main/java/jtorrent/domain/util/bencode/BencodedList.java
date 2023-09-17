package jtorrent.domain.util.bencode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BencodedList extends ArrayList<Object> {

    public BencodedList(List<?> list) {
        super(list);
    }

    public ByteBuffer getBytes(int index) {
        return get(index, ByteBuffer.class);
    }

    public Optional<ByteBuffer> getOptionalBytes(int index) {
        return getOptional(index, ByteBuffer.class);
    }

    public String getString(int index) {
        ByteBuffer buffer = getBytes(index);
        return new String(buffer.array(), StandardCharsets.UTF_8);
    }

    public Optional<String> getOptionalString(int index) {
        return getOptionalBytes(index).map(buffer -> new String(buffer.array(), StandardCharsets.UTF_8));
    }

    public int getInt(int index) {
        return (int) getLong(index);
    }

    public Optional<Integer> getOptionalInt(int index) {
        return getOptionalLong(index).map(Long::intValue);
    }

    public long getLong(int index) {
        return get(index, Long.class);
    }

    public Optional<Long> getOptionalLong(int index) {
        return getOptional(index, Long.class);
    }

    public BencodedMap getMap(int index) {
        try {
            return new BencodedMap(get(index, Map.class));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Value at index " + index + " is not a map");
        }
    }

    public BencodedList getList(int index) {
        return new BencodedList(get(index, List.class));
    }

    private <T> T get(int index, Class<T> clazz) {
        Object value = get(index);

        try {
            return clazz.cast(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Value at index " + index + " is not of type " + clazz.getName());
        }
    }

    private <T> Optional<T> getOptional(int index, Class<T> clazz) {
        Object value = get(index);

        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(clazz.cast(value));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Value at index " + index + " is not of type " + clazz.getName());
        }
    }
}
