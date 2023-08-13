package jtorrent.domain.util;

import java.util.List;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.core.Observer;

public class RxObservableList<E> extends RxObservableCollectionBase<E, List<E>, OrderedCollectionEvent<E>> {

    public RxObservableList(List<E> list) {
        super(list);
    }

    public E get(int index) {
        try {
            rLock.lock();
            return collection.get(index);
        } finally {
            rLock.unlock();
        }
    }

    @Override
    protected void add(E item) {
        try {
            wLock.lock();
            collection.add(item);
            notifyAdded(item, collection.size() - 1);
        } finally {
            wLock.unlock();
        }
    }

    protected void add(int index, E item) {
        try {
            wLock.lock();
            collection.add(index, item);
            notifyAdded(item, index);
        } finally {
            wLock.unlock();
        }
    }

    @Override
    protected void remove(E item) {
        try {
            wLock.lock();
            int index = collection.indexOf(item);

            if (index != -1) {
                collection.remove(index);
                notifyRemoved(item, index);
            }
        } finally {
            wLock.unlock();
        }
    }

    protected void remove(int index) {
        try {
            wLock.lock();
            E item = collection.remove(index);
            notifyRemoved(item, index);
        } finally {
            wLock.unlock();
        }
    }

    private void notifyRemoved(E item, int index) {
        emitEvent(OrderedCollectionEvent.remove(item, index));
    }

    @Override
    protected void emitInitialState(Observer<? super OrderedCollectionEvent<E>> observer) {
        IntStream.range(0, collection.size())
                .forEach(i -> observer.onNext(OrderedCollectionEvent.add(collection.get(i), i)));
    }

    private void notifyAdded(E item, int index) {
        emitEvent(OrderedCollectionEvent.add(item, index));
    }

    @Override
    protected void notifyCleared() {
        emitEvent(OrderedCollectionEvent.clear());
    }
}
