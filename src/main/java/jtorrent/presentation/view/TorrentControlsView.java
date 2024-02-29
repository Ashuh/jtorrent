package jtorrent.presentation.view;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import jtorrent.presentation.model.UiNewTorrent;
import jtorrent.presentation.model.UiTorrent;
import jtorrent.presentation.model.UiTorrentStatus;
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

    private ViewModel viewModel;

    public void setViewModel(ViewModel viewModel) {
        this.viewModel = viewModel;
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

        ObservableValue<Boolean> isStartButtonDisabled = viewModel.selectedTorrentProperty()
                .flatMap(UiTorrent::statusProperty)
                .flatMap(UiTorrentStatus::stateProperty)
                .map(state -> !state.contains("STOPPED"))
                .orElse(true);

        ObservableValue<Boolean> isStopButtonDisabled = viewModel.selectedTorrentProperty()
                .flatMap(UiTorrent::statusProperty)
                .flatMap(UiTorrentStatus::stateProperty)
                .map(state -> state.contains("STOPPED"))
                .orElse(true);

        startButton.disableProperty()
                .bind(Bindings.createBooleanBinding(isStartButtonDisabled::getValue, isStartButtonDisabled));
        stopButton.disableProperty()
                .bind(Bindings.createBooleanBinding(isStopButtonDisabled::getValue, isStopButtonDisabled));
    }

    @FXML
    private void initialize() {
        createButton.setOnMouseClicked(mouseEvent -> {
            CreateNewTorrentDialog dialog = new CreateNewTorrentDialog();
            dialog.initOwner(createButton.getScene().getWindow());
            Optional<UiNewTorrent> result = dialog.showAndWait();
            if (result.isPresent()) {
                // TODO: create new torrent
            }
        });
    }
}
