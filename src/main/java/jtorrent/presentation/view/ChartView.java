package jtorrent.presentation.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import jtorrent.presentation.model.UiChartData;

public class ChartView {

    @FXML
    private LineChart<Number, Number> chart;
    @FXML
    private NumberAxis timeAxis;
    @FXML
    private NumberAxis rateAxis;

    private final ObjectProperty<UiChartData> chartData = new SimpleObjectProperty<>();

    @FXML
    private void initialize() {
        chart.dataProperty().bind(chartData.map(UiChartData::getChartData));
        timeAxis.lowerBoundProperty().bind(chartData.flatMap(UiChartData::lowerBoundProperty));
        timeAxis.upperBoundProperty().bind(chartData.flatMap(UiChartData::upperBoundProperty));
        rateAxis.tickLabelFormatterProperty().bind(chartData.flatMap(UiChartData::rateAxisFormatterProperty));
    }

    public ObjectProperty<UiChartData> chartDataProperty() {
        return chartData;
    }
}
