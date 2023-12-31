package jtorrent.application.presentation.view;

import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;
import jtorrent.application.presentation.viewmodel.ViewModel;
import jtorrent.peer.presentation.view.PeersTableView;
import jtorrent.torrent.presentation.view.FilesView;
import jtorrent.torrent.presentation.view.TorrentInfoView;
import jtorrent.torrent.presentation.view.TorrentsTableView;

public class MainWindow {

    @FXML
    private MenuBar menuBar;
    @FXML
    private TorrentsTableView torrentsTableViewController;
    @FXML
    private PeersTableView peersTableViewController;
    @FXML
    private TorrentInfoView torrentInfoViewController;
    @FXML
    private FilesView filesViewController;

    public void setViewModel(ViewModel viewModel) {
        torrentsTableViewController.setViewModel(viewModel);
        peersTableViewController.setViewModel(viewModel);
        filesViewController.itemsProperty().bind(viewModel.getFileInfos());
        torrentInfoViewController.torrentInfoProperty().bind(viewModel.getTorrentInfo());
    }
}
