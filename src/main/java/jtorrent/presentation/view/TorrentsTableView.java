package jtorrent.presentation.view;

import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import jtorrent.presentation.model.UiTorrent;

public class TorrentsTableView extends UiComponent {

    @FXML
    private TableView<UiTorrent> tableView;
    @FXML
    private TableColumn<UiTorrent, String> name;
    @FXML
    private TableColumn<UiTorrent, Long> size;
    @FXML
    private TableColumn<UiTorrent, Double> status;
    @FXML
    private TableColumn<UiTorrent, Double> downSpeed;
    @FXML
    private TableColumn<UiTorrent, Double> upSpeed;
    @FXML
    private TableColumn<UiTorrent, Double> eta;

    public TorrentsTableView() {
        name.setCellValueFactory(cd -> cd.getValue().nameProperty());
        size.setCellValueFactory(cd -> cd.getValue().sizeProperty().asObject());
        status.setCellValueFactory(param -> param.getValue().progressProperty().asObject());
        status.setCellFactory(ProgressBarTableCell.forTableColumn());
        downSpeed.setCellValueFactory(cd -> cd.getValue().downSpeedProperty().asObject());
        upSpeed.setCellValueFactory(cd -> cd.getValue().upSpeedProperty().asObject());
        eta.setCellValueFactory(cd -> cd.getValue().etaProperty().asObject());
    }

    public TableView.TableViewSelectionModel<UiTorrent> getSelectionModel() {
        return tableView.getSelectionModel();
    }

    public void setItems(ObservableList<UiTorrent> torrents) {
        SortedList<UiTorrent> sortedList = new SortedList<>(torrents);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
    }
}
