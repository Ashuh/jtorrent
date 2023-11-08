package jtorrent.common.domain.util.rx;

import static java.util.Objects.requireNonNull;

public class CollectionEvent<T> {

    private final Type type;
    private final T item;

    protected CollectionEvent(Type type, T item) {
        this.type = requireNonNull(type);
        this.item = item;
    }

    public static <T> CollectionEvent<T> add(T item) {
        return new CollectionEvent<>(Type.ADD, requireNonNull(item));
    }

    public static <T> CollectionEvent<T> remove(T item) {
        return new CollectionEvent<>(Type.REMOVE, requireNonNull(item));
    }

    public static <E> CollectionEvent<E> clear() {
        return new CollectionEvent<>(Type.CLEAR, null);
    }

    public Type getType() {
        return type;
    }

    public T getItem() {
        return item;
    }

    public enum Type {
        ADD,
        REMOVE,
        CLEAR
    }
}
