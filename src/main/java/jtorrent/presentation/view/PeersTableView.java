package jtorrent.presentation.view;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jtorrent.presentation.model.UiPeer;
import jtorrent.presentation.viewmodel.ViewModel;

public class PeersTableView implements Initializable {

    @FXML
    private TableView<UiPeer> tableView;
    @FXML
    private TableColumn<UiPeer, String> ip;
    @FXML
    private TableColumn<UiPeer, String> port;
    @FXML
    private TableColumn<UiPeer, String> client;
    @FXML
    private TableColumn<UiPeer, String> peerDownSpeed;
    @FXML
    private TableColumn<UiPeer, String> peerUpSpeed;

    private ViewModel viewModel;

    public void setViewModel(ViewModel viewModel) {
        this.viewModel = viewModel;
        SortedList<UiPeer> sortedList = new SortedList<>(viewModel.getPeers());
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ip.setCellValueFactory(cd -> cd.getValue().ipProperty());
        port.setCellValueFactory(cd -> cd.getValue().portProperty());
        client.setCellValueFactory(cd -> cd.getValue().clientProperty());
        peerDownSpeed.setCellValueFactory(cd -> cd.getValue().downSpeedProperty());
        peerUpSpeed.setCellValueFactory(cd -> cd.getValue().upSpeedProperty());

        tableView.setContextMenu(new ContextMenu(initMenuItems()));
    }

    private MenuItem[] initMenuItems() {
        MenuItem addPeer = new MenuItem("Add Peer");
        addPeer.setOnAction(event -> {
            if (!viewModel.hasSelectedTorrent()) {
                return;
            }

            Dialog<String> dialog = new PeerInputDialog();
            dialog.initOwner(tableView.getScene().getWindow());
            Optional<String> input = dialog.showAndWait();
            input.ifPresent(ipPort -> {
                try {
                    viewModel.addPeerForSelectedTorrent(ipPort);
                } catch (UnknownHostException | IllegalArgumentException e) {
                    showExceptionDialog(e);
                }
            });
        });

        return new MenuItem[] {addPeer};
    }

    private void showExceptionDialog(Exception e) {
        ExceptionAlert alert = new ExceptionAlert("Error", "Failed to add peer", e);
        alert.showAndWait();
    }
}
