package jtorrent.domain.util;

import static java.util.Objects.requireNonNull;

import java.util.Collection;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * An {@link Observable} that wraps a {@link Collection} and emits events when the collection is modified.
 *
 * @param <E> the type of elements in this collection
 * @param <T> the type of the collection
 * @param <V> the type of the event
 */
public abstract class RxObservableCollectionBase<E, T extends Collection<E>, V> extends Observable<V> {

    protected final PublishSubject<V> publishSubject = PublishSubject.create();
    protected final T collection;

    protected RxObservableCollectionBase(T collection) {
        this.collection = requireNonNull(collection);
    }

    protected void add(E item) {
        if (collection.add(item)) {
            notifyAdded(item);
        }
    }

    protected void remove(E item) {
        if (collection.remove(item)) {
            notifyRemoved(item);
        }
    }

    protected void clear() {
        collection.clear();
        notifyCleared();
    }

    public boolean containsItem(E item) {
        return collection.contains(item);
    }

    protected void emitEvent(V event) {
        publishSubject.onNext(event);
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super V> observer) {
        // TODO: need to make this thread safe
        publishSubject.subscribe(observer);
        emitInitialState();
    }

    protected abstract void emitInitialState();

    protected abstract void notifyAdded(E item);

    protected abstract void notifyRemoved(E item);

    protected abstract void notifyCleared();
}
