package jtorrent.presentation.main.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.common.component.TorrentStatusCell;
import jtorrent.presentation.common.util.BindingUtils;
import jtorrent.presentation.common.util.DataSize;
import jtorrent.presentation.main.util.CalculateEtaCombiner;

public class UiTorrent {

    private final ReadOnlyStringWrapper name;
    private final ReadOnlyStringWrapper size;
    private final ReadOnlyStringWrapper downSpeed;
    private final ReadOnlyStringWrapper upSpeed;
    private final ReadOnlyStringWrapper eta;
    private final ReadOnlyStringWrapper saveDirectory;
    private final ReadOnlyObjectWrapper<TorrentStatusCell.Status> status;
    private final CompositeDisposable disposables;

    public UiTorrent(ReadOnlyStringWrapper name, ReadOnlyStringWrapper size, ReadOnlyStringWrapper downSpeed,
            ReadOnlyStringWrapper upSpeed, ReadOnlyStringWrapper eta, ReadOnlyStringWrapper saveDirectory,
            ReadOnlyObjectWrapper<TorrentStatusCell.Status> status, CompositeDisposable disposables) {
        this.name = requireNonNull(name);
        this.size = requireNonNull(size);
        this.downSpeed = requireNonNull(downSpeed);
        this.upSpeed = requireNonNull(upSpeed);
        this.eta = requireNonNull(eta);
        this.saveDirectory = requireNonNull(saveDirectory);
        this.status = requireNonNull(status);
        this.disposables = requireNonNull(disposables);
    }

    public static UiTorrent fromDomain(Torrent torrent) {
        long torrentSize = torrent.getTotalSize();

        ReadOnlyStringWrapper name = new ReadOnlyStringWrapper(torrent.getName());
        ReadOnlyStringWrapper size = new ReadOnlyStringWrapper(DataSize.bestFitBytes(torrentSize).toString());
        ReadOnlyStringWrapper downSpeed = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper upSpeed = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper eta = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper saveDirectory = new ReadOnlyStringWrapper(torrent.getSaveDirectory().toString());
        ReadOnlyStringWrapper state = new ReadOnlyStringWrapper();
        DoubleProperty progress = new SimpleDoubleProperty(0.0);
        ReadOnlyObjectWrapper<TorrentStatusCell.Status> status =
                new ReadOnlyObjectWrapper<>(new TorrentStatusCell.Status(state, progress));
        CompositeDisposable disposables = new CompositeDisposable();

        Observable<Double> downloadRateObservable = torrent.getDownloadRateObservable();
        BindingUtils.subscribe(downloadRateObservable.map(UiTorrent::formatRate), downSpeed, disposables);

        Observable<Double> uploadRateObservable = torrent.getUploadRateObservable();
        BindingUtils.subscribe(uploadRateObservable.map(UiTorrent::formatRate), upSpeed, disposables);

        Observable<Long> verifiedBytesObservable = torrent.getVerifiedBytesObservable();
        Observable<String> etaObservable = Observable.combineLatest(verifiedBytesObservable, downloadRateObservable,
                new CalculateEtaCombiner(torrentSize));
        BindingUtils.subscribe(etaObservable, eta, disposables);

        Observable<Double> downloadProgressObservable = torrent.getVerifiedBytesObservable()
                .map(verifiedBytes -> (double) verifiedBytes / torrentSize);

        Observable<Double> checkProgressObservable = torrent.getCheckedBytesObservable()
                .map(checkedBytes -> (double) checkedBytes / torrentSize);

        Observable<Torrent.State> stateObservable = torrent.getStateObservable();

        Observable<Double> progressObservable = Observable.combineLatest(
                stateObservable, downloadProgressObservable, checkProgressObservable,
                UiTorrent::combineProgress);
        BindingUtils.subscribe(progressObservable, progress, disposables);

        Observable<String> statusObservable = Observable.combineLatest(
                stateObservable, progressObservable, UiTorrent::combineStatus);
        BindingUtils.subscribe(statusObservable, state, disposables);

        return new UiTorrent(name, size, downSpeed, upSpeed, eta, saveDirectory, status, disposables);
    }

    private static String formatRate(double bytes) {
        if (bytes == 0) {
            return "";
        }
        return DataSize.bestFitBytes(bytes).toRateString();
    }

    private static double combineProgress(Torrent.State state, double downloadProgress, double checkProgress) {
        return state == Torrent.State.CHECKING ? checkProgress : downloadProgress;
    }

    private static String combineStatus(Torrent.State state, Double progress) {
        if (state == Torrent.State.CHECKING || state == Torrent.State.DOWNLOADING) {
            return state + " " + String.format("%.1f", progress * 100) + "%";
        }
        return state.toString();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty sizeProperty() {
        return size.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty downSpeedProperty() {
        return downSpeed.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty upSpeedProperty() {
        return upSpeed.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty etaProperty() {
        return eta.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty saveDirectoryProperty() {
        return saveDirectory.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<TorrentStatusCell.Status> statusProperty() {
        return status.getReadOnlyProperty();
    }

    public void dispose() {
        disposables.dispose();
    }
}
