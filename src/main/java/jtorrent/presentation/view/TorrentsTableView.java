package jtorrent.presentation.view;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import jtorrent.presentation.model.UiTorrent;
import jtorrent.presentation.model.UiTorrentStatus;
import jtorrent.presentation.viewmodel.ViewModel;

public class TorrentsTableView implements Initializable {

    @FXML
    private TableView<UiTorrent> tableView;
    @FXML
    private TableColumn<UiTorrent, String> name;
    @FXML
    private TableColumn<UiTorrent, String> size;
    @FXML
    private TableColumn<UiTorrent, UiTorrentStatus> status;
    @FXML
    private TableColumn<UiTorrent, String> downSpeed;
    @FXML
    private TableColumn<UiTorrent, String> upSpeed;
    @FXML
    private TableColumn<UiTorrent, String> eta;
    @FXML
    private TableColumn<UiTorrent, String> saveDirectory;

    private ViewModel viewModel;

    public void setViewModel(ViewModel viewModel) {
        this.viewModel = viewModel;
        SortedList<UiTorrent> sortedList = new SortedList<>(viewModel.getTorrents());
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
        tableView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> viewModel.setTorrentSelected(newValue));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        name.setCellValueFactory(cd -> cd.getValue().nameProperty());
        size.setCellValueFactory(cd -> cd.getValue().sizeProperty());
        status.setCellValueFactory(param -> param.getValue().statusProperty());
        status.setCellFactory(param -> new TorrentStatusCell());
        downSpeed.setCellValueFactory(cd -> cd.getValue().downSpeedProperty());
        upSpeed.setCellValueFactory(cd -> cd.getValue().upSpeedProperty());
        eta.setCellValueFactory(cd -> cd.getValue().etaProperty());
        saveDirectory.setCellValueFactory(cd -> cd.getValue().saveDirectoryProperty());
        tableView.setRowFactory(param -> new TorrentTableRow());
    }

    private class TorrentTableRow extends TableRow<UiTorrent> {

        public TorrentTableRow() {
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !isEmpty()) {
                    UiTorrent torrent = getItem();
                    viewModel.showTorrentInFileExplorer(torrent);
                }
            });
        }
    }
}
