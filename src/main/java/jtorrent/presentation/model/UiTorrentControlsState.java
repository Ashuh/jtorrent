package jtorrent.presentation.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.util.Optional;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.util.BindingUtils;

public class UiTorrentControlsState {

    private final BooleanProperty startDisabled;
    private final BooleanProperty stopDisabled;
    private final CompositeDisposable disposables;

    public UiTorrentControlsState(BooleanProperty startDisabled, BooleanProperty stopDisabled,
            CompositeDisposable disposables) {
        this.startDisabled = requireNonNull(startDisabled);
        this.stopDisabled = requireNonNull(stopDisabled);
        this.disposables = requireNonNull(disposables);
    }

    public static UiTorrentControlsState build(Observable<Optional<Torrent>> selectedTorrentObservable) {
        BooleanProperty startDisabled = new SimpleBooleanProperty(true);
        BooleanProperty stopDisabled = new SimpleBooleanProperty(true);
        CompositeDisposable disposables = new CompositeDisposable();

        Observable<State> stateObservable = selectedTorrentObservable
                .map(optionalTorrent -> optionalTorrent
                        .map(Torrent::getStateObservable)
                        .map(x -> x.map(State::fromTorrentState))
                )
                .flatMap(optionalObservable -> optionalObservable.orElse(Observable.just(State.NONE_SELECTED)));

        Observable<Boolean> startDisabledObservable = stateObservable
                .map(x -> x == State.STARTED || x == State.NONE_SELECTED);
        BindingUtils.subscribe(startDisabledObservable, startDisabled, disposables);

        Observable<Boolean> stopDisabledObservable = stateObservable
                .map(x -> x == State.STOPPED || x == State.NONE_SELECTED);
        BindingUtils.subscribe(stopDisabledObservable, stopDisabled, disposables);

        return new UiTorrentControlsState(startDisabled, stopDisabled, disposables);
    }

    public ObservableValue<Boolean> startDisabledProperty() {
        return startDisabled;
    }

    public ObservableValue<Boolean> stopDisabledProperty() {
        return stopDisabled;
    }

    public void dispose() {
        disposables.dispose();
    }

    private enum State {
        NONE_SELECTED,
        STOPPED,
        STARTED;

        public static State fromTorrentState(Torrent.State state) {
            return requireNonNull(state) == Torrent.State.STOPPED ? STOPPED : STARTED;
        }
    }
}
