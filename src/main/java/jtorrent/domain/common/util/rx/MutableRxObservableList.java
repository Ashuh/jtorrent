package jtorrent.domain.common.util.rx;

import java.util.List;

public class MutableRxObservableList<E> extends RxObservableList<E> {

    public MutableRxObservableList(List<E> list) {
        super(list);
    }

    @Override
    public void add(E item) {
        super.add(item);
    }

    @Override
    public void add(int index, E item) {
        super.add(index, item);
    }

    @Override
    public void remove(E item) {
        super.remove(item);
    }

    @Override
    public void remove(int index) {
        super.remove(index);
    }

    @Override
    public void clear() {
        super.clear();
    }
}
