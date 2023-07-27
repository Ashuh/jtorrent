package jtorrent.domain.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class RxObservableList<T> extends Observable<ListEvent<T>> {

    protected final List<T> list = new ArrayList<>();
    protected final PublishSubject<ListEvent<T>> publishSubject = PublishSubject.create();

    public List<T> getList() {
        return Collections.unmodifiableList(list);
    }

    public T get(int index) {
        return list.get(index);
    }

    protected void add(T item) {
        list.add(item);
        notifySubscribers(new ListEvent<>(ListEvent.Type.ADD, item, list.size() - 1));
    }

    protected void add(int index, T item) {
        list.add(index, item);
        notifySubscribers(new ListEvent<>(ListEvent.Type.ADD, item, index));
    }

    protected void remove(T item) {
        int index = list.indexOf(item);

        if (index != -1) {
            list.remove(index);
            notifySubscribers(new ListEvent<>(ListEvent.Type.REMOVE, item, index));
        }
    }

    protected void remove(int index) {
        T item = list.remove(index);
        notifySubscribers(new ListEvent<>(ListEvent.Type.REMOVE, item, index));
    }

    protected void notifySubscribers(ListEvent<T> event) {
        publishSubject.onNext(event);
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super ListEvent<T>> observer) {
        IntStream.range(0, list.size())
                .forEach(index -> observer.onNext(new ListEvent<>(ListEvent.Type.ADD, list.get(index), index)));
        publishSubject.subscribe(observer);
    }
}
