package jtorrent.domain.util;

import java.util.Collection;

public abstract class RxObservableCollection<E>
        extends RxObservableCollectionBase<E, Collection<E>, CollectionEvent<E>> {

    protected RxObservableCollection(Collection<E> collection) {
        super(collection);
    }

    @Override
    protected void notifyAdded(E item) {
        emitEvent(CollectionEvent.add(item));
    }

    @Override
    protected void notifyRemoved(E item) {
        emitEvent(CollectionEvent.remove(item));
    }

    @Override
    protected void notifyCleared() {
        emitEvent(CollectionEvent.clear());
    }

    @Override
    protected void emitInitialState() {
        collection.forEach(this::notifyAdded);
    }
}
