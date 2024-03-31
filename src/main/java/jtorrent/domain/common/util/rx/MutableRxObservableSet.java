package jtorrent.domain.common.util.rx;

import java.util.Set;

public class MutableRxObservableSet<E> extends RxObservableSet<E> {

    public MutableRxObservableSet(Set<E> set) {
        super(set);
    }

    @Override
    public void add(E item) {
        super.add(item);
    }

    @Override
    public void remove(E item) {
        super.remove(item);
    }

    @Override
    public void clear() {
        super.clear();
    }
}
