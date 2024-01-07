package jtorrent.torrent.presentation;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.common.presentation.util.BindingUtils;
import jtorrent.common.presentation.util.DataUnitFormatter;
import jtorrent.torrent.domain.model.Torrent;

public class UiTorrent {

    private final StringProperty name;
    private final StringProperty size;
    private final DoubleProperty progress;
    private final StringProperty downSpeed;
    private final StringProperty upSpeed;
    private final StringProperty eta;
    private final StringProperty saveDirectory;
    private final BooleanProperty isActive;
    private final CompositeDisposable disposables;

    public UiTorrent(StringProperty name, StringProperty size, DoubleProperty progress, StringProperty downSpeed,
            StringProperty upSpeed, StringProperty eta, StringProperty saveDirectory, BooleanProperty isActive,
            CompositeDisposable disposables) {
        this.name = requireNonNull(name);
        this.size = requireNonNull(size);
        this.progress = requireNonNull(progress);
        this.downSpeed = requireNonNull(downSpeed);
        this.upSpeed = requireNonNull(upSpeed);
        this.eta = requireNonNull(eta);
        this.saveDirectory = requireNonNull(saveDirectory);
        this.isActive = requireNonNull(isActive);
        this.disposables = disposables;
    }

    public static UiTorrent fromDomain(Torrent torrent) {
        long torrentSize = torrent.getTotalSize();

        StringProperty name = new SimpleStringProperty(torrent.getName());
        StringProperty size = new SimpleStringProperty(DataUnitFormatter.formatSize(torrentSize));
        DoubleProperty progress = new SimpleDoubleProperty(0.0);
        StringProperty downSpeed = new SimpleStringProperty("");
        StringProperty upSpeed = new SimpleStringProperty("");
        StringProperty eta = new SimpleStringProperty("");
        StringProperty saveDirectory = new SimpleStringProperty(torrent.getSaveDirectory().toString());
        BooleanProperty isActive = new SimpleBooleanProperty(false);
        CompositeDisposable disposables = new CompositeDisposable();

        Observable<Double> downloadRateObservable = torrent.getDownloadRateObservable();
        BindingUtils.subscribe(downloadRateObservable.map(UiTorrent::formatRate), downSpeed, disposables);

        Observable<Double> uploadRateObservable = torrent.getUploadRateObservable();
        BindingUtils.subscribe(uploadRateObservable.map(UiTorrent::formatRate), upSpeed, disposables);

        Observable<Long> verifiedBytesObservable = torrent.getVerifiedBytesObservable();
        Observable<String> etaObservable = Observable.combineLatest(verifiedBytesObservable, downloadRateObservable,
                new CalculateEtaCombiner(torrentSize));
        BindingUtils.subscribe(etaObservable, eta, disposables);

        Observable<Double> progressObservable = torrent.getDownloadedObservable()
                .map(verifiedBytes -> (double) verifiedBytes / torrentSize);
        BindingUtils.subscribe(progressObservable, progress, disposables);

        Observable<Boolean> isActiveObservable = torrent.getIsActiveObservable();
        BindingUtils.subscribe(isActiveObservable, isActive, disposables);

        return new UiTorrent(name, size, progress, downSpeed, upSpeed, eta, saveDirectory, isActive, disposables);
    }

    private static String formatRate(double bytes) {
        if (bytes == 0) {
            return "";
        }
        return DataUnitFormatter.formatRate(bytes);
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

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
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

    public boolean isActive() {
        return isActive.get();
    }

    public BooleanProperty isActiveProperty() {
        return isActive;
    }

    public void dispose() {
        disposables.dispose();
    }
}
