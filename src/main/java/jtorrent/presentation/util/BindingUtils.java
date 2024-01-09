package jtorrent.presentation.util;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.beans.property.Property;

public class BindingUtils {

    private BindingUtils() {
    }

    public static <T> void subscribe(Observable<T> observable, Property<? super T> property,
            CompositeDisposable disposables) {
        Disposable disposable = observable.subscribe(new UpdatePropertyConsumer<>(property));
        disposables.add(disposable);
    }
}
