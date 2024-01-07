package jtorrent.torrent.presentation;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import io.reactivex.rxjava3.core.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.common.presentation.util.DataUnitFormatter;
import jtorrent.common.presentation.util.UpdatePropertyConsumer;
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

    public UiTorrent(StringProperty name, StringProperty size, DoubleProperty progress, StringProperty downSpeed,
            StringProperty upSpeed, StringProperty eta, StringProperty saveDirectory, BooleanProperty isActive) {
        this.name = requireNonNull(name);
        this.size = requireNonNull(size);
        this.progress = requireNonNull(progress);
        this.downSpeed = requireNonNull(downSpeed);
        this.upSpeed = requireNonNull(upSpeed);
        this.eta = requireNonNull(eta);
        this.saveDirectory = requireNonNull(saveDirectory);
        this.isActive = requireNonNull(isActive);
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

        Observable<Integer> downloadedObservable = torrent.getDownloadedObservable();
        Observable<Double> downloadRateObservable = torrent.getDownloadRateObservable();
        Observable<Double> uploadRateObservable = torrent.getUploadRateObservable();
        Observable<Boolean> isActiveObservable = torrent.getIsActiveObservable();
        Observable<Long> verifiedBytesObservable = torrent.getVerifiedBytesObservable();
        downloadRateObservable
                .map(UiTorrent::formatRate)
                .subscribe(new UpdatePropertyConsumer<>(downSpeed));
        uploadRateObservable
                .map(UiTorrent::formatRate)
                .subscribe(new UpdatePropertyConsumer<>(upSpeed));
        Observable.combineLatest(verifiedBytesObservable, downloadRateObservable,
                        new CalculateEtaCombiner(torrentSize))
                .subscribe(new UpdatePropertyConsumer<>(eta));

        verifiedBytesObservable
                .map(verifiedBytes -> (double) verifiedBytes / torrentSize)
                .subscribe(new UpdatePropertyConsumer<>(progress));

        isActiveObservable.subscribe(new UpdatePropertyConsumer<>(isActive));

        return new UiTorrent(name, size, progress, downSpeed, upSpeed, eta, saveDirectory, isActive);
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

}
