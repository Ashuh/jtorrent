package jtorrent.peer.presentation;

import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jtorrent.common.presentation.UiComponent;
import jtorrent.peer.presentation.UiPeer;

public class PeersTableView extends UiComponent {

    @FXML
    private TableView<UiPeer> tableView;
    @FXML
    private TableColumn<UiPeer, String> ip;
    @FXML
    private TableColumn<UiPeer, String> client;
    @FXML
    private TableColumn<UiPeer, String> peerDownSpeed;
    @FXML
    private TableColumn<UiPeer, String> peerUpSpeed;

    public PeersTableView() {
        ip.setCellValueFactory(cd -> cd.getValue().ipProperty());
        client.setCellValueFactory(cd -> cd.getValue().clientProperty());
        peerDownSpeed.setCellValueFactory(cd -> cd.getValue().downSpeedProperty().asString());
        peerUpSpeed.setCellValueFactory(cd -> cd.getValue().upSpeedProperty().asString());
    }

    public void setItems(ObservableList<UiPeer> torrents) {
        SortedList<UiPeer> sortedList = new SortedList<>(torrents);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
    }
}
