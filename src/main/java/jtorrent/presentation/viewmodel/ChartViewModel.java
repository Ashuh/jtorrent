package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;
import jtorrent.domain.Client;
import jtorrent.presentation.util.DataSize;

public class ChartViewModel {

    private static final int MAX_DATA_POINTS = 60;
    private static final int BOUND_GAP = MAX_DATA_POINTS - 1;

    private final Client client;
    private final ObservableList<XYChart.Series<Number, Number>> chartData =
            FXCollections.observableList(new LinkedList<>());
    private final ReadOnlyLongWrapper lowerBound = new ReadOnlyLongWrapper();
    private final ReadOnlyLongWrapper upperBound = new ReadOnlyLongWrapper();
    private final ReadOnlyObjectWrapper<StringConverter<Number>> rateAxisFormatter =
            new ReadOnlyObjectWrapper<>(new RateAxisFormatter());
    private final Disposable disposable;

    public ChartViewModel(Client client) {
        this.client = requireNonNull(client);

        upperBound.bind(lowerBound.add(BOUND_GAP));

        XYChart.Series<Number, Number> downloadRateSeries = createSeries("Download");
        chartData.add(downloadRateSeries);
        ObservableList<XYChart.Data<Number, Number>> downloadRateData = downloadRateSeries.getData();

        XYChart.Series<Number, Number> uploadRateSeries = createSeries("Upload");
        chartData.add(uploadRateSeries);
        ObservableList<XYChart.Data<Number, Number>> uploadRateData = uploadRateSeries.getData();

        disposable = Observable.interval(1000, TimeUnit.MILLISECONDS, Schedulers.computation())
                .subscribe(tick -> {
                    double downloadRate = client.getDownloadRate();
                    double uploadRate = client.getUploadRate();

                    Platform.runLater(() -> {
                        addDataPoint(downloadRateData, tick, downloadRate);
                        addDataPoint(uploadRateData, tick, uploadRate);

                        lowerBound.set(Math.max(0, tick - BOUND_GAP));
                    });
                });
    }

    private static void addDataPoint(ObservableList<XYChart.Data<Number, Number>> data, long x, double y) {
        if (data.size() >= MAX_DATA_POINTS) {
            data.remove(0);
        }
        data.add(new XYChart.Data<>(x, y));
    }

    private static XYChart.Series<Number, Number> createSeries(String name) {
        ObservableList<XYChart.Data<Number, Number>> data = FXCollections.observableList(new LinkedList<>());
        return new XYChart.Series<>(name, data);
    }

    public ObservableList<XYChart.Series<Number, Number>> getChartData() {
        return FXCollections.unmodifiableObservableList(chartData);
    }

    public ReadOnlyLongProperty lowerBoundProperty() {
        return lowerBound.getReadOnlyProperty();
    }

    public ReadOnlyLongProperty upperBoundProperty() {
        return upperBound.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<StringConverter<Number>> rateAxisFormatterProperty() {
        return rateAxisFormatter.getReadOnlyProperty();
    }

    private static class RateAxisFormatter extends StringConverter<Number> {

        @Override
        public String toString(Number object) {
            return DataSize.bestFitBytes(object.longValue()).toRateString();
        }

        @Override
        public Number fromString(String string) {
            return DataSize.fromRateString(string).getSizeInBytes();
        }
    }
}
