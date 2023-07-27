package jtorrent.domain.util;

public class ListEvent<T> {

    private final Type eventType;
    private final T item;
    private final int index;

    public ListEvent(Type eventType, T item, int index) {
        this.eventType = eventType;
        this.item = item;
        this.index = index;
    }

    public Type getEventType() {
        return eventType;
    }

    public T getItem() {
        return item;
    }

    public int getIndex() {
        return index;
    }

    public enum Type {
        ADD,
        REMOVE
    }
}
