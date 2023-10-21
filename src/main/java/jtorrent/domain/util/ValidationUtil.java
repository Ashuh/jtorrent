package jtorrent.domain.util;

public class ValidationUtil {

    private ValidationUtil() {
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
        return obj;
    }

    public static int requireAtLeast(int value, int min) {
        if (value < min) {
            throw new IllegalArgumentException("Value must be at least " + min);
        }
        return value;
    }

    public static int requireAtMost(int value, int max) {
        if (value > max) {
            throw new IllegalArgumentException("Value must be at most " + max);
        }
        return value;
    }

    public static int requireInRange(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Value must be in range [" + min + ", " + max + "]");
        }
        return value;
    }

    public static int requirePositive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Value must be positive");
        }
        return value;
    }
}
