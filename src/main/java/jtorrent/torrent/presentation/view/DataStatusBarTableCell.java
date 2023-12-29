package jtorrent.torrent.presentation.view;

import java.util.BitSet;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import jtorrent.torrent.presentation.UiFileInfo;

public class DataStatusBarTableCell extends TableCell<UiFileInfo, DataStatusBarTableCell.State> {

    private final DataStatusBar dataStatusBar = new DataStatusBar();

    @Override
    protected void updateItem(State item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            dataStatusBar.availabilityProperty().unbind();

            final TableColumn<UiFileInfo, State> column = getTableColumn();
            ObservableValue<State> observable = column == null ? null : column.getCellObservableValue(getIndex());

            if (observable != null) {
                dataStatusBar.totalSegmentsProperty().bind(observable.getValue().totalSegments());
                dataStatusBar.availabilityProperty().bind(observable.getValue().availability());
            } else if (item != null) {
                dataStatusBar.setTotalSegments(item.totalSegments.get());
                dataStatusBar.setAvailability(item.availability.get());
            }

            VBox hBox = new VBox();
            hBox.setAlignment(Pos.CENTER);
            hBox.getChildren().add(dataStatusBar);
            setGraphic(hBox);
        }
    }

    public record State(IntegerProperty totalSegments, ObjectProperty<BitSet> availability) {
    }
}
