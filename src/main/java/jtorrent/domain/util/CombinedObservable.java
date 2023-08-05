package jtorrent.domain.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

public abstract class CombinedObservable<T, R> extends Observable<R> {

    private final Map<Observable<T>, Observable<T>> originalToActual = new HashMap<>();
    private final Set<ObservableSource<T>> sources = new HashSet<>();
    private final Subject<Collection<ObservableSource<T>>> sourcesSubject = BehaviorSubject.create();
    private final Observable<R> combined;

    protected CombinedObservable() {
        combined = sourcesSubject.switchMap(this::combineLatest);
    }

    public void addSource(Observable<T> source) {
        Observable<T> actual = source.doOnComplete(() -> removeSource(source));
        originalToActual.put(source, actual);
        sources.add(actual);
        sourcesSubject.onNext(sources);
    }

    public void removeSource(Observable<T> source) {
        Observable<T> actualSource = originalToActual.remove(source);
        sources.remove(actualSource);
        sourcesSubject.onNext(sources);
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super R> observer) {
        combined.subscribe(observer);
    }

    private ObservableSource<? extends R> combineLatest(Collection<ObservableSource<T>> sources) {
        return Observable.combineLatest(sources, this::combine);
    }

    protected abstract R combine(Object[] objects);
}