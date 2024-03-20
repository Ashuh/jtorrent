package jtorrent.presentation.main.view;

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
import javafx.scene.input.ContextMenuEvent;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;
import jtorrent.presentation.exception.view.ExceptionAlert;
import jtorrent.presentation.main.model.UiPeer;
import jtorrent.presentation.main.viewmodel.PeersTableViewModel;
import jtorrent.presentation.peerinput.view.PeerInputDialog;

public class PeersTableView extends TableView<UiPeer> {

    private final ObjectProperty<PeersTableViewModel> viewModel = new SimpleObjectProperty<>();
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

    public ObjectProperty<PeersTableViewModel> viewModelProperty() {
        return viewModel;
    }

    @FXML
    public void initialize() {
        addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            PeersTableViewModel vm = viewModel.get();
            if (vm == null || !vm.hasSelectedTorrent()) {
                event.consume();
            }
        });

        setContextMenu(new ContextMenu(new AddPeerMenuItem()));
        itemsProperty().bind(viewModel
                .map(PeersTableViewModel::getUiPeers)
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
            private final PeersTableViewModel viewModel;

            private AddPeerEventHandler(PeersTableViewModel viewModel) {
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
