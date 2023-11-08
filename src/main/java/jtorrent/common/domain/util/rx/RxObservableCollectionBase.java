package jtorrent.common.domain.util.rx;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

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

    protected final T collection;
    protected final PublishSubject<V> publishSubject = PublishSubject.create();
    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    protected final Lock rLock = rwLock.readLock();
    protected final Lock wLock = rwLock.writeLock();

    protected RxObservableCollectionBase(T collection) {
        this.collection = requireNonNull(collection);
    }

    protected void clear() {
        collection.clear();
        notifyCleared();
    }

    public Collection<E> getCollection() {
        return Collections.unmodifiableCollection(collection);
    }

    public boolean containsItem(E item) {
        return collection.contains(item);
    }

    public boolean anyMatch(Predicate<E> predicate) {
        try {
            rLock.lock();
            return collection.stream().anyMatch(predicate);
        } finally {
            rLock.unlock();
        }
    }

    protected void emitEvent(V event) {
        publishSubject.onNext(event);
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super V> observer) {
        try {
            rLock.lock();
            emitInitialState(observer);
            publishSubject.subscribe(observer);
        } finally {
            rLock.unlock();
        }
    }

    protected abstract void add(E item);

    protected abstract void remove(E item);

    protected abstract void emitInitialState(Observer<? super V> observer);

    protected abstract void notifyCleared();
}
