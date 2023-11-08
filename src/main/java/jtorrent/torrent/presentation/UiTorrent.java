package jtorrent.torrent.presentation;

import static java.util.Objects.requireNonNull;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.BiFunction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.common.presentation.util.UpdatePropertyConsumer;

public class UiTorrent {

    private final StringProperty name;
    private final LongProperty size;
    private final DoubleProperty progress;
    private final DoubleProperty downSpeed;
    private final DoubleProperty upSpeed;
    private final DoubleProperty eta;
    private final BooleanProperty isActive;

    public UiTorrent(StringProperty name, LongProperty size, DoubleProperty progress, DoubleProperty downSpeed,
            DoubleProperty upSpeed, DoubleProperty eta, BooleanProperty isActive) {
        this.name = requireNonNull(name);
        this.size = requireNonNull(size);
        this.progress = requireNonNull(progress);
        this.downSpeed = requireNonNull(downSpeed);
        this.upSpeed = requireNonNull(upSpeed);
        this.eta = requireNonNull(eta);
        this.isActive = requireNonNull(isActive);
    }

    public static UiTorrent fromDomain(Torrent torrent) {
        long torrentSize = torrent.getTotalSize();

        StringProperty name = new SimpleStringProperty(torrent.getName());
        LongProperty size = new SimpleLongProperty(torrentSize);
        DoubleProperty progress = new SimpleDoubleProperty(0.0);
        DoubleProperty downSpeed = new SimpleDoubleProperty(0.0);
        DoubleProperty upSpeed = new SimpleDoubleProperty(0.0);
        DoubleProperty eta = new SimpleDoubleProperty(Double.POSITIVE_INFINITY);
        BooleanProperty isActive = new SimpleBooleanProperty(false);

        Observable<Integer> downloadedObservable = torrent.getDownloadedObservable();
        Observable<Double> downloadRateObservable = torrent.getDownloadRateObservable();
        Observable<Boolean> isActiveObservable = torrent.getIsActiveObservable();

        downloadRateObservable.subscribe(new UpdatePropertyConsumer<>(downSpeed));
        Observable.combineLatest(downloadedObservable, downloadRateObservable, new CalculateEtaCombiner(torrentSize))
                .subscribe(new UpdatePropertyConsumer<>(eta));
        downloadedObservable.subscribe(downloaded -> progress.set((double) downloaded / torrentSize));
        isActiveObservable.subscribe(new UpdatePropertyConsumer<>(isActive));

        return new UiTorrent(name, size, progress, downSpeed, upSpeed, eta, isActive);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public long getSize() {
        return size.get();
    }

    public LongProperty sizeProperty() {
        return size;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public double getDownSpeed() {
        return downSpeed.get();
    }

    public DoubleProperty downSpeedProperty() {
        return downSpeed;
    }

    public double getUpSpeed() {
        return upSpeed.get();
    }

    public DoubleProperty upSpeedProperty() {
        return upSpeed;
    }

    public double getEta() {
        return eta.get();
    }

    public DoubleProperty etaProperty() {
        return eta;
    }

    public boolean isActive() {
        return isActive.get();
    }

    public BooleanProperty isActiveProperty() {
        return isActive;
    }

    private static class CalculateEtaCombiner implements BiFunction<Integer, Double, Double> {

        private final long size;

        public CalculateEtaCombiner(long size) {
            this.size = size;
        }

        @Override
        public Double apply(Integer downloaded, Double rate) {
            if (rate == 0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return (size - downloaded) / rate;
            }
        }
    }
}
