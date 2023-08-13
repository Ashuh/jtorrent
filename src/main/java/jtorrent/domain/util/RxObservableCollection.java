package jtorrent.domain.util;

import java.util.Collection;

import io.reactivex.rxjava3.core.Observer;

public abstract class RxObservableCollection<E>
        extends RxObservableCollectionBase<E, Collection<E>, CollectionEvent<E>> {

    protected RxObservableCollection(Collection<E> collection) {
        super(collection);
    }

    @Override
    protected void add(E item) {
        if (collection.add(item)) {
            notifyAdded(item);
        }
    }

    @Override
    protected void remove(E item) {
        if (collection.remove(item)) {
            notifyRemoved(item);
        }
    }

    private void notifyRemoved(E item) {
        emitEvent(CollectionEvent.remove(item));
    }

    @Override
    protected void emitInitialState(Observer<? super CollectionEvent<E>> observer) {
        collection.forEach(item -> observer.onNext(CollectionEvent.add(item)));
    }

    @Override
    protected void notifyCleared() {
        emitEvent(CollectionEvent.clear());
    }

    private void notifyAdded(E item) {
        emitEvent(CollectionEvent.add(item));
    }
}
