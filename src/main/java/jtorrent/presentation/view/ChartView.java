package jtorrent.presentation.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import jtorrent.presentation.viewmodel.ChartViewModel;

public class ChartView extends LineChart<Number, Number> {

    private final ObjectProperty<ChartViewModel> viewModel = new SimpleObjectProperty<>();

    public ChartView() {
        super(buildTimeAxis(), buildRateAxis());
        setCreateSymbols(false);
        setAnimated(false);
        dataProperty().bind(viewModel.map(ChartViewModel::getChartData));
        getTimeAxis().lowerBoundProperty().bind(viewModel.flatMap(ChartViewModel::lowerBoundProperty));
        getTimeAxis().upperBoundProperty().bind(viewModel.flatMap(ChartViewModel::upperBoundProperty));
        getRateAxis().tickLabelFormatterProperty().bind(viewModel.flatMap(ChartViewModel::rateAxisFormatterProperty));
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

    public ObjectProperty<ChartViewModel> viewModelProperty() {
        return viewModel;
    }
}
