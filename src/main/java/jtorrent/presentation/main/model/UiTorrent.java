package jtorrent.presentation.main.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.common.component.TorrentStatusCell;
import jtorrent.presentation.common.util.BindingUtils;
import jtorrent.presentation.common.util.DataSize;
import jtorrent.presentation.main.util.CalculateEtaCombiner;

public class UiTorrent {

    private final StringProperty name;
    private final StringProperty size;
    private final StringProperty downSpeed;
    private final StringProperty upSpeed;
    private final StringProperty eta;
    private final StringProperty saveDirectory;
    private final ObjectProperty<TorrentStatusCell.Status> status;
    private final CompositeDisposable disposables;

    public UiTorrent(StringProperty name, StringProperty size, StringProperty downSpeed,
            StringProperty upSpeed, StringProperty eta, StringProperty saveDirectory,
            ObjectProperty<TorrentStatusCell.Status> status, CompositeDisposable disposables) {
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

        StringProperty name = new SimpleStringProperty(torrent.getName());
        StringProperty size = new SimpleStringProperty(DataSize.bestFitBytes(torrentSize).toString());
        StringProperty downSpeed = new SimpleStringProperty("");
        StringProperty upSpeed = new SimpleStringProperty("");
        StringProperty eta = new SimpleStringProperty("");
        StringProperty saveDirectory = new SimpleStringProperty(torrent.getSaveDirectory().toString());
        StringProperty state = new SimpleStringProperty();
        DoubleProperty progress = new SimpleDoubleProperty(0.0);
        ObjectProperty<TorrentStatusCell.Status> status =
                new SimpleObjectProperty<>(new TorrentStatusCell.Status(state, progress));
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

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getSize() {
        return size.get();
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public String getDownSpeed() {
        return downSpeed.get();
    }

    public StringProperty downSpeedProperty() {
        return downSpeed;
    }

    public String getUpSpeed() {
        return upSpeed.get();
    }

    public StringProperty upSpeedProperty() {
        return upSpeed;
    }

    public String getEta() {
        return eta.get();
    }

    public StringProperty etaProperty() {
        return eta;
    }

    public String getSaveDirectory() {
        return saveDirectory.get();
    }

    public StringProperty saveDirectoryProperty() {
        return saveDirectory;
    }

    public ObjectProperty<TorrentStatusCell.Status> statusProperty() {
        return status;
    }

    public void dispose() {
        disposables.dispose();
    }
}
