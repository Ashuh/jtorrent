package jtorrent.domain.common.util.rx;

import java.util.Set;

public class RxObservableSet<E> extends RxObservableCollection<E> {

    public RxObservableSet(Set<E> set) {
        super(set);
    }
}
