package jtorrent.domain.util;

public class MutableRxObservableList<T> extends RxObservableList<T> {

    @Override
    public void add(T item) {
        super.add(item);
    }

    @Override
    public void add(int index, T item) {
        super.add(index, item);
    }

    @Override
    public void remove(T item) {
        super.remove(item);
    }

    @Override
    public void remove(int index) {
        super.remove(index);
    }
}
