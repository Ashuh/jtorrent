package jtorrent.presentation.view;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import jtorrent.presentation.model.UiNewTorrent;
import jtorrent.presentation.model.UiTorrentControlsState;
import jtorrent.presentation.viewmodel.ViewModel;

public class TorrentControlsView {

    @FXML
    private Button addButton;
    @FXML
    private Button addUrlButton;
    @FXML
    private Button createButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;

    private final ObjectProperty<UiTorrentControlsState> torrentControlsState = new SimpleObjectProperty<>();

    public void setViewModel(ViewModel viewModel) {
        startButton.setOnMouseClicked(event -> viewModel.startSelectedTorrent());
        stopButton.setOnMouseClicked(event -> viewModel.stopSelectedTorrent());
        addButton.setOnMouseClicked(new AddNewTorrentFileEventHandler<>(viewModel) {
            @Override
            protected boolean shouldHandle(MouseEvent event) {
                return event.getButton() == MouseButton.PRIMARY;
            }

            @Override
            protected Window getOwnerWindow() {
                return addButton.getScene().getWindow();
            }
        });
    }

    @FXML
    private void initialize() {
        startButton.disableProperty()
                .bind(torrentControlsState
                        .flatMap(UiTorrentControlsState::startDisabledProperty)
                        .orElse(true));
        stopButton.disableProperty()
                .bind(torrentControlsState
                        .flatMap(UiTorrentControlsState::stopDisabledProperty)
                        .orElse(true));

        createButton.setOnMouseClicked(mouseEvent -> {
            CreateNewTorrentDialog dialog = new CreateNewTorrentDialog();
            dialog.initOwner(createButton.getScene().getWindow());
            Optional<UiNewTorrent> result = dialog.showAndWait();
            if (result.isPresent()) {
                // TODO: create new torrent
            }
        });
    }

    public ObjectProperty<UiTorrentControlsState> torrentControlsStateProperty() {
        return torrentControlsState;
    }
}
