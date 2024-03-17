package jtorrent.presentation.main.view;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
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
import jtorrent.data.torrent.model.BencodedTorrent;
import jtorrent.presentation.addnewtorrent.view.AddNewTorrentDialog;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;
import jtorrent.presentation.main.util.AddNewTorrentFileEventHandler;
import jtorrent.presentation.main.util.AddNewTorrentUrlEventHandler;
import jtorrent.presentation.main.viewmodel.MainViewModel;
import jtorrent.presentation.preferences.view.PreferencesDialog;

public class MainView extends BorderPane {

    private final ObjectProperty<MainViewModel> viewModel = new SimpleObjectProperty<>();
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
    private FileInfoView fileInfoView;
    @FXML
    private ChartView chartView;

    public MainView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void setViewModel(MainViewModel viewModel) {
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

        torrentControlsView.viewModelProperty().bind(viewModel.map(MainViewModel::getTorrentControlsViewModel));
        torrentsTableView.viewModelProperty().bind(viewModel.map(MainViewModel::getTorrentsTableViewModel));
        torrentInfoView.viewModelProperty().bind(viewModel.map(MainViewModel::getTorrentInfoViewModel));
        fileInfoView.viewModelProperty().bind(viewModel.map(MainViewModel::getFileInfoViewModel));
        peersTableView.viewModelProperty().bind(viewModel.map(MainViewModel::getPeersTableViewModel));
        chartView.viewModelProperty().bind(viewModel.map(MainViewModel::getChartViewModel));
    }

    private class AddFileButtonEventHandler extends AddNewTorrentFileEventHandler<ActionEvent> {

        private final MainViewModel viewModel;

        private AddFileButtonEventHandler(MainViewModel viewModel) {
            this.viewModel = requireNonNull(viewModel);
        }

        @Override
        protected Window getOwnerWindow() {
            return getScene().getWindow();
        }

        @Override
        protected void addTorrent(BencodedTorrent bencodedTorrent, AddNewTorrentDialog.Result result) {
            viewModel.addTorrent(bencodedTorrent, result);
        }

        @Override
        protected boolean shouldHandle(ActionEvent event) {
            return true;
        }

        @Override
        protected BencodedTorrent loadTorrent(File file) throws IOException {
            return viewModel.loadTorrent(file);
        }
    }

    private class AddUrlButtonEventHandler extends AddNewTorrentUrlEventHandler<ActionEvent> {

        private final MainViewModel viewModel;

        private AddUrlButtonEventHandler(MainViewModel viewModel) {
            this.viewModel = requireNonNull(viewModel);
        }

        @Override
        protected Window getOwnerWindow() {
            return getScene().getWindow();
        }

        @Override
        protected void addTorrent(BencodedTorrent bencodedTorrent, AddNewTorrentDialog.Result result) {
            viewModel.addTorrent(bencodedTorrent, result);
        }

        @Override
        protected boolean shouldHandle(ActionEvent event) {
            return true;
        }

        @Override
        protected BencodedTorrent loadTorrent(String string) throws IOException {
            return viewModel.loadTorrent(string);
        }
    }
}
