package jtorrent.application.presentation.view;

import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;
import jtorrent.application.presentation.viewmodel.ViewModel;
import jtorrent.peer.presentation.view.PeersTableView;
import jtorrent.torrent.presentation.view.TorrentsTableView;

public class MainWindow {

    @FXML
    private MenuBar menuBar;
    @FXML
    private TorrentsTableView torrentsTableViewController;
    @FXML
    private PeersTableView peersTableViewController;

    public void setViewModel(ViewModel viewModel) {
        torrentsTableViewController.setItems(viewModel.getTorrents());
        torrentsTableViewController.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> viewModel.setTorrentSelected(newValue));
        torrentsTableViewController.setOnStartButtonClickedCallback(viewModel::startSelectedTorrent);
        torrentsTableViewController.setOnStopButtonClickedCallback(viewModel::stopSelectedTorrent);
        peersTableViewController.setItems(viewModel.getPeers());
    }
}
