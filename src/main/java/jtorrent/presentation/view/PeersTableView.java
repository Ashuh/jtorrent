package jtorrent.presentation.view;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jtorrent.presentation.model.UiPeer;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;
import jtorrent.presentation.viewmodel.ViewModel;

public class PeersTableView extends TableView<UiPeer> {

    private final ObjectProperty<ViewModel> viewModel = new SimpleObjectProperty<>();
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

    public PeersTableView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public ObjectProperty<ViewModel> viewModelProperty() {
        return viewModel;
    }

    @FXML
    public void initialize() {
        setContextMenu(new ContextMenu(new AddPeerMenuItem()));
        itemsProperty().bind(viewModel
                .map(ViewModel::getPeers)
                .map(SortedList::new)
                .map(sortedList -> {
                    sortedList.comparatorProperty().bind(comparatorProperty());
                    return sortedList;
                }));

        ip.setCellValueFactory(param -> param.getValue().ipProperty());
        port.setCellValueFactory(param -> param.getValue().portProperty());
        client.setCellValueFactory(param -> param.getValue().clientProperty());
        peerDownSpeed.setCellValueFactory(param -> param.getValue().downSpeedProperty());
        peerUpSpeed.setCellValueFactory(param -> param.getValue().upSpeedProperty());
    }

    private class AddPeerMenuItem extends MenuItem {

        public AddPeerMenuItem() {
            super("Add Peer");
            onActionProperty().bind(viewModel.map(AddPeerEventHandler::new));
        }

        private class AddPeerEventHandler implements EventHandler<ActionEvent> {
            private final ViewModel viewModel;

            private AddPeerEventHandler(ViewModel viewModel) {
                this.viewModel = requireNonNull(viewModel);
            }

            @Override
            public void handle(ActionEvent event) {
                if (!viewModel.hasSelectedTorrent()) {
                    return;
                }

                Dialog<String> dialog = new PeerInputDialog();
                dialog.initOwner(getScene().getWindow());
                Optional<String> input = dialog.showAndWait();
                input.ifPresent(ipPort -> {
                    try {
                        viewModel.addPeerForSelectedTorrent(ipPort);
                    } catch (UnknownHostException | IllegalArgumentException e) {
                        showExceptionDialog(e);
                    }
                });
            }

            private void showExceptionDialog(Exception e) {
                ExceptionAlert alert = new ExceptionAlert("Error", "Failed to add peer", e);
                alert.showAndWait();
            }
        }
    }
}
