package jtorrent.presentation.view;

import java.io.IOException;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;
import jtorrent.presentation.viewmodel.ViewModel;

public class MainWindow extends BorderPane {

    private final ObjectProperty<ViewModel> viewModel = new SimpleObjectProperty<>();
    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu fileMenu;
    @FXML
    private MenuItem addTorrentFromFile;
    @FXML
    private MenuItem addTorrentFromUrl;
    @FXML
    private MenuItem preferences;
    @FXML
    private MenuItem quit;
    @FXML
    private TorrentControlsView torrentControlsView;
    @FXML
    private TorrentsTableView torrentsTableView;
    @FXML
    private PeersTableView peersTableView;
    @FXML
    private TorrentInfoView torrentInfoView;
    @FXML
    private FilesView filesView;
    @FXML
    private ChartView chartView;

    public MainWindow() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void setViewModel(ViewModel viewModel) {
        this.viewModel.set(viewModel);
    }

    @FXML
    private void initialize() {
        menuBar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });

        quit.setOnAction(event -> Platform.exit());
        preferences.setOnAction(event -> {
            PreferencesDialog preferencesDialog = new PreferencesDialog();
            preferencesDialog.initOwner(menuBar.getScene().getWindow());
            preferencesDialog.showAndWait();
        });

        addTorrentFromFile.onActionProperty().bind(viewModel.map(AddFileButtonEventHandler::new));
        addTorrentFromUrl.onActionProperty().bind(viewModel.map(AddUrlButtonEventHandler::new));

        torrentControlsView.viewModelProperty().bind(viewModel);
        torrentsTableView.viewModelProperty().bind(viewModel.map(ViewModel::getTorrentsTableViewModel));
        torrentInfoView.viewModelProperty().bind(viewModel);
        filesView.viewModelProperty().bind(viewModel);
        peersTableView.viewModelProperty().bind(viewModel.map(ViewModel::getPeersTableViewModel));
        chartView.viewModelProperty().bind(viewModel);
    }

    private class AddFileButtonEventHandler extends AddNewTorrentFileEventHandler<ActionEvent> {

        private AddFileButtonEventHandler(ViewModel viewModel) {
            super(viewModel);
        }

        @Override
        protected Window getOwnerWindow() {
            return getScene().getWindow();
        }

        @Override
        protected boolean shouldHandle(ActionEvent event) {
            return true;
        }
    }

    private class AddUrlButtonEventHandler extends AddNewTorrentUrlEventHandler<ActionEvent> {

        private AddUrlButtonEventHandler(ViewModel viewModel) {
            super(viewModel);
        }

        @Override
        protected Window getOwnerWindow() {
            return getScene().getWindow();
        }

        @Override
        protected boolean shouldHandle(ActionEvent event) {
            return true;
        }
    }
}
