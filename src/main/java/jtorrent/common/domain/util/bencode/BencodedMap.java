package jtorrent.common.domain.util.bencode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.dampcake.bencode.BencodeInputStream;

public class BencodedMap extends HashMap<String, Object> {

    public BencodedMap(Map<String, ?> map) {
        super(map);
    }

    public static BencodedMap decode(byte[] bytes) throws IOException {
        return decode(new ByteArrayInputStream(bytes));
    }

    public static BencodedMap decode(InputStream inputStream) throws IOException {
        BencodeInputStream bis = new BencodeInputStream(inputStream, StandardCharsets.UTF_8, true);
        return new BencodedMap(bis.readDictionary());
    }

    public ByteBuffer getBytes(String key) {
        return get(key, ByteBuffer.class);
    }

    public Optional<ByteBuffer> getOptionalBytes(String key) {
        return getOptional(key, ByteBuffer.class);
    }

    public char getChar(String key) {
        String value = getString(key);
        if (value.length() != 1) {
            throw new IllegalArgumentException("Value for key " + key + " is not a char: " + value);
        }
        return value.charAt(0);
    }

    public Optional<Character> getOptionalChar(String key) {
        Optional<String> value = getOptionalString(key);
        if (value.isPresent() && value.get().length() != 1) {
            throw new IllegalArgumentException("Value for key " + key + " is not a char: " + value.get());
        }
        return value.map(v -> v.charAt(0));
    }

    public String getString(String key) {
        ByteBuffer buffer = getBytes(key);
        return new String(buffer.array(), StandardCharsets.UTF_8);
    }

    public Optional<String> getOptionalString(String key) {
        return getOptionalBytes(key).map(buffer -> new String(buffer.array(), StandardCharsets.UTF_8));
    }

    public short getShort(String key) {
        return (short) getLong(key);
    }

    public Optional<Short> getOptionalShort(String key) {
        return getOptionalLong(key).map(Long::shortValue);
    }

    public int getInt(String key) {
        return (int) getLong(key);
    }

    public Optional<Integer> getOptionalInt(String key) {
        return getOptionalLong(key).map(Long::intValue);
    }

    public long getLong(String key) {
        return get(key, Long.class);
    }

    public Optional<Long> getOptionalLong(String key) {
        return getOptional(key, Long.class);
    }

    @SuppressWarnings("unchecked")
    public BencodedMap getMap(String key) {
        try {
            return new BencodedMap(get(key, Map.class));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Value for key " + key + " is not a map");
        }
    }

    public BencodedList getList(String key) {
        return new BencodedList(get(key, List.class));
    }

    public Optional<BencodedList> getOptionalList(String key) {
        return getOptional(key, List.class).map(BencodedList::new);
    }

    private <T> T get(String key, Class<T> clazz) {
        if (!containsKey(key)) {
            throw new NoSuchElementException("Key not found: " + key);
        }

        Object value = get(key);

        if (value == null) {
            throw new NoSuchElementException("Value for key " + key + " is null");
        }

        try {
            return clazz.cast(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Value for key " + key + " is not of type " + clazz.getName());
        }
    }

    private <T> Optional<T> getOptional(String key, Class<T> clazz) {
        Object value = get(key);

        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(clazz.cast(value));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Value for key " + key + " is not of type " + clazz.getName());
        }
    }
}
