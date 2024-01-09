package jtorrent.presentation.view;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import jtorrent.presentation.viewmodel.ViewModel;

public class MainWindow {

    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu fileMenu;
    @FXML
    private MenuItem newMenuItem;
    @FXML
    private TorrentsTableView torrentsTableViewController;
    @FXML
    private PeersTableView peersTableViewController;
    @FXML
    private TorrentInfoView torrentInfoViewController;
    @FXML
    private FilesView filesViewController;

    public void setViewModel(ViewModel viewModel) {
        menuBar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });

        newMenuItem.setOnAction(new AddNewTorrentEventHandler<>(viewModel) {
            @Override
            protected boolean shouldHandle(ActionEvent event) {
                return true;
            }

            @Override
            protected Window getOwnerWindow() {
                return menuBar.getScene().getWindow();
            }
        });

        torrentsTableViewController.setViewModel(viewModel);
        peersTableViewController.setViewModel(viewModel);
        filesViewController.itemsProperty().bind(viewModel.getFileInfos());
        torrentInfoViewController.torrentInfoProperty().bind(viewModel.getTorrentInfo());
    }
}
