package jtorrent.presentation.view;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import jtorrent.presentation.model.UiTorrent;
import jtorrent.presentation.viewmodel.ViewModel;

public class TorrentsTableView implements Initializable {

    @FXML
    private TableView<UiTorrent> tableView;
    @FXML
    private TableColumn<UiTorrent, String> name;
    @FXML
    private TableColumn<UiTorrent, String> size;
    @FXML
    private TableColumn<UiTorrent, Double> status;
    @FXML
    private TableColumn<UiTorrent, String> downSpeed;
    @FXML
    private TableColumn<UiTorrent, String> upSpeed;
    @FXML
    private TableColumn<UiTorrent, String> eta;
    @FXML
    private TableColumn<UiTorrent, String> saveDirectory;

    @FXML
    private Button addButton;
    @FXML
    private Button addUrlButton;
    @FXML
    private Button createFileButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;

    private ViewModel viewModel;

    public void setViewModel(ViewModel viewModel) {
        this.viewModel = viewModel;
        SortedList<UiTorrent> sortedList = new SortedList<>(viewModel.getTorrents());
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
        tableView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> viewModel.setTorrentSelected(newValue));
        startButton.setOnMouseClicked(event -> viewModel.startSelectedTorrent());
        stopButton.setOnMouseClicked(event -> viewModel.stopSelectedTorrent());
        addButton.setOnMouseClicked(new AddNewTorrentEventHandler<>(viewModel) {
            @Override
            protected boolean shouldHandle(MouseEvent event) {
                return event.getButton() == MouseButton.PRIMARY;
            }

            @Override
            protected Window getOwnerWindow() {
                return addButton.getScene().getWindow();
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        name.setCellValueFactory(cd -> cd.getValue().nameProperty());
        size.setCellValueFactory(cd -> cd.getValue().sizeProperty());
        status.setCellValueFactory(param -> param.getValue().progressProperty().asObject());
        status.setCellFactory(ProgressBarTableCell.forTableColumn());
        downSpeed.setCellValueFactory(cd -> cd.getValue().downSpeedProperty());
        upSpeed.setCellValueFactory(cd -> cd.getValue().upSpeedProperty());
        eta.setCellValueFactory(cd -> cd.getValue().etaProperty());
        saveDirectory.setCellValueFactory(cd -> cd.getValue().saveDirectoryProperty());

        StartButtonDisabledBinding startButtonDisabledBinding =
                new StartButtonDisabledBinding(tableView.getSelectionModel().selectedItemProperty());
        startButton.disableProperty().bind(startButtonDisabledBinding);

        StopButtonDisabledBinding stopButtonDisabledBinding =
                new StopButtonDisabledBinding(tableView.getSelectionModel().selectedItemProperty());
        stopButton.disableProperty().bind(stopButtonDisabledBinding);

        tableView.setRowFactory(param -> new TorrentTableRow());
    }

    private abstract static class ButtonDisabledBinding extends BooleanBinding implements ChangeListener<UiTorrent> {

        protected UiTorrent selectedTorrent;

        public ButtonDisabledBinding(ReadOnlyObjectProperty<UiTorrent> selectedTorrent) {
            selectedTorrent.addListener(this);
        }

        @Override
        protected boolean computeValue() {
            return selectedTorrent != null && selectedTorrent.isActiveProperty().get();
        }

        @Override
        public void changed(ObservableValue<? extends UiTorrent> observable, UiTorrent oldValue, UiTorrent newValue) {
            if (oldValue != null) {
                unbind(oldValue.isActiveProperty());
            }
            if (newValue != null) {
                bind(newValue.isActiveProperty());
            }
            selectedTorrent = newValue;
            invalidate();
        }
    }

    private static class StartButtonDisabledBinding extends ButtonDisabledBinding {

        public StartButtonDisabledBinding(ReadOnlyObjectProperty<UiTorrent> selectedTorrent) {
            super(selectedTorrent);
        }

        @Override
        protected boolean computeValue() {
            return selectedTorrent == null || selectedTorrent.isActiveProperty().get();
        }
    }

    private static class StopButtonDisabledBinding extends ButtonDisabledBinding {

        public StopButtonDisabledBinding(ReadOnlyObjectProperty<UiTorrent> selectedTorrent) {
            super(selectedTorrent);
        }

        @Override
        protected boolean computeValue() {
            return selectedTorrent == null || !selectedTorrent.isActiveProperty().get();
        }
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
