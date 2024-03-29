package jtorrent.application.presentation.view;

import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;
import jtorrent.application.presentation.viewmodel.ViewModel;
import jtorrent.common.presentation.UiComponent;
import jtorrent.peer.presentation.view.PeersTableView;
import jtorrent.torrent.presentation.view.TorrentsTableView;

public class MainWindow extends UiComponent {

    @FXML
    private MenuBar menuBar;
    @FXML
    private TorrentsTableView torrentsTableView;
    @FXML
    private PeersTableView peersTableView;

    public MainWindow(ViewModel viewModel) {
        torrentsTableView.setItems(viewModel.getTorrents());
        torrentsTableView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> viewModel.setTorrentSelected(newValue));
        peersTableView.setItems(viewModel.getPeers());
        torrentsTableView.setOnStartButtonClickedCallback(viewModel::startSelectedTorrent);
        torrentsTableView.setOnStopButtonClickedCallback(viewModel::stopSelectedTorrent);
    }
}
