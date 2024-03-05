package jtorrent.presentation.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import jtorrent.presentation.model.UiChartData;
import jtorrent.presentation.viewmodel.ViewModel;

public class ChartView extends LineChart<Number, Number> {

    private final ObjectProperty<ViewModel> viewModel = new SimpleObjectProperty<>();

    public ChartView() {
        super(buildTimeAxis(), buildRateAxis());
        setCreateSymbols(false);
        setAnimated(false);
        ObservableValue<UiChartData> uiChartData = viewModel.flatMap(ViewModel::chartDataProperty);
        dataProperty().bind(uiChartData.map(UiChartData::getChartData));
        getTimeAxis().lowerBoundProperty().bind(uiChartData.flatMap(UiChartData::lowerBoundProperty));
        getTimeAxis().upperBoundProperty().bind(uiChartData.flatMap(UiChartData::upperBoundProperty));
        getRateAxis().tickLabelFormatterProperty().bind(uiChartData.flatMap(UiChartData::rateAxisFormatterProperty));
    }

    private static NumberAxis buildTimeAxis() {
        NumberAxis timeAxis = new NumberAxis();
        timeAxis.setAnimated(false);
        timeAxis.setAutoRanging(false);
        timeAxis.setMinorTickVisible(false);
        timeAxis.setTickLabelsVisible(false);
        timeAxis.setTickMarkVisible(false);
        timeAxis.setSide(Side.BOTTOM);
        return timeAxis;
    }

    private static NumberAxis buildRateAxis() {
        NumberAxis rateAxis = new NumberAxis();
        rateAxis.setAnimated(false);
        rateAxis.setMinorTickVisible(false);
        rateAxis.setSide(Side.LEFT);
        return rateAxis;
    }

    private NumberAxis getTimeAxis() {
        return (NumberAxis) getXAxis();
    }

    private NumberAxis getRateAxis() {
        return (NumberAxis) getYAxis();
    }

    public ObjectProperty<ViewModel> viewModelProperty() {
        return viewModel;
    }
}
