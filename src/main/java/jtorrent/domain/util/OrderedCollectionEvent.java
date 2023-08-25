package jtorrent.domain.util;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

public class OrderedCollectionEvent<T> extends CollectionEvent<T> {

    private final Integer index;

    public OrderedCollectionEvent(Type type, T item, Integer index) {
        super(type, item);
        this.index = index;
    }

    public static <T> OrderedCollectionEvent<T> add(T item, int index) {
        return new OrderedCollectionEvent<>(Type.ADD, requireNonNull(item), index);
    }

    public static <T> OrderedCollectionEvent<T> remove(T item, int index) {
        return new OrderedCollectionEvent<>(Type.REMOVE, requireNonNull(item), index);
    }

    public static <E> OrderedCollectionEvent<E> clear() {
        return new OrderedCollectionEvent<>(Type.CLEAR, null, null);
    }

    public Optional<Integer> getIndex() {
        return Optional.ofNullable(index);
    }
}
