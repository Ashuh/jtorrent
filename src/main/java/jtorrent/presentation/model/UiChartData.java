package jtorrent.presentation.model;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;
import jtorrent.domain.Client;
import jtorrent.presentation.util.DataUnit;

public class UiChartData {

    private static final int MAX_DATA_POINTS = 60;
    private static final int BOUND_GAP = MAX_DATA_POINTS - 1;

    private final ObservableList<XYChart.Series<Number, Number>> chartData;
    private final LongProperty lowerBound;
    private final LongProperty upperBound;
    private final ObjectProperty<StringConverter<Number>> rateAxisFormatter =
            new SimpleObjectProperty<>(new RateAxisFormatter());
    private final Disposable disposable;

    private UiChartData(ObservableList<XYChart.Series<Number, Number>> chartData, LongProperty lowerBound,
            LongProperty upperBound, Disposable disposable) {
        this.chartData = chartData;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.disposable = disposable;
    }

    public static UiChartData build(Client client) {
        ObservableList<XYChart.Series<Number, Number>> chartData = FXCollections.observableList(new LinkedList<>());
        LongProperty lowerBound = new SimpleLongProperty();
        LongProperty upperBound = new SimpleLongProperty();
        upperBound.bind(lowerBound.add(BOUND_GAP));

        XYChart.Series<Number, Number> downloadRateSeries = createSeries("Download");
        chartData.add(downloadRateSeries);
        ObservableList<XYChart.Data<Number, Number>> downloadRateData = downloadRateSeries.getData();

        XYChart.Series<Number, Number> uploadRateSeries = createSeries("Upload");
        chartData.add(uploadRateSeries);
        ObservableList<XYChart.Data<Number, Number>> uploadRateData = uploadRateSeries.getData();

        Disposable disposable = Observable.interval(1000, TimeUnit.MILLISECONDS, Schedulers.computation())
                .subscribe(tick -> {
                    double downloadRate = client.getDownloadRate();
                    double uploadRate = client.getUploadRate();

                    Platform.runLater(() -> {
                        addDataPoint(downloadRateData, tick, downloadRate);
                        addDataPoint(uploadRateData, tick, uploadRate);

                        lowerBound.set(Math.max(0, tick - BOUND_GAP));
                    });
                });

        return new UiChartData(chartData, lowerBound, upperBound, disposable);
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
        return lowerBound;
    }

    public ReadOnlyLongProperty upperBoundProperty() {
        return upperBound;
    }

    public ObjectProperty<StringConverter<Number>> rateAxisFormatterProperty() {
        return rateAxisFormatter;
    }

    public void dispose() {
        disposable.dispose();
    }

    private static class RateAxisFormatter extends StringConverter<Number> {
        @Override
        public String toString(Number object) {
            DataUnit unit = DataUnit.forBytes(object.longValue());
            double value = unit.convertFrom(object.doubleValue(), DataUnit.BYTE);
            return String.format("%.2f %s", value, unit.getRateSymbol());
        }

        @Override
        public Number fromString(String string) {
            String[] tokens = string.split(" ");
            double value = Double.parseDouble(tokens[0]);
            DataUnit unit = DataUnit.fromRateSymbol(tokens[1]);
            return unit.convertTo(value, DataUnit.BYTE);
        }
    }
}
