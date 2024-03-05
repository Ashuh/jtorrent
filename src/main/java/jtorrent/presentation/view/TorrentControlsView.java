package jtorrent.presentation.view;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import jtorrent.presentation.model.UiNewTorrent;
import jtorrent.presentation.model.UiTorrentControlsState;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;
import jtorrent.presentation.viewmodel.ViewModel;

public class TorrentControlsView extends ToolBar {

    private final ObjectProperty<ViewModel> viewModel = new SimpleObjectProperty<>();
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

    public TorrentControlsView() {
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
    private void initialize() {
        ObservableValue<UiTorrentControlsState> torrentControlsState = viewModel
                .flatMap(ViewModel::torrentControlsStateProperty);

        ObservableValue<Boolean> startButtonDisabledObservable = torrentControlsState
                .flatMap(UiTorrentControlsState::startDisabledProperty)
                .orElse(true);
        startButton.disableProperty().bind(startButtonDisabledObservable);

        ObservableValue<Boolean> stopButtonDisabledObservable = torrentControlsState
                .flatMap(UiTorrentControlsState::stopDisabledProperty)
                .orElse(true);
        stopButton.disableProperty().bind(stopButtonDisabledObservable);

        startButton.onMouseClickedProperty().bind(viewModel.map(StartButtonMouseEventHandler::new));
        stopButton.onMouseClickedProperty().bind(viewModel.map(StopButtonMouseEventHandler::new));
        addButton.onMouseClickedProperty().bind(viewModel.map(AddButtonMouseEventHandler::new));

        createButton.setOnMouseClicked(mouseEvent -> {
            CreateNewTorrentDialog dialog = new CreateNewTorrentDialog();
            dialog.initOwner(createButton.getScene().getWindow());
            Optional<UiNewTorrent> result = dialog.showAndWait();
            if (result.isPresent()) {
                // TODO: create new torrent
            }
        });
    }

    private static class StartButtonMouseEventHandler implements EventHandler<MouseEvent> {
        private final ViewModel viewModel;

        private StartButtonMouseEventHandler(ViewModel viewModel) {
            this.viewModel = requireNonNull(viewModel);
        }

        @Override
        public void handle(MouseEvent event) {
            viewModel.startSelectedTorrent();
        }
    }

    private static class StopButtonMouseEventHandler implements EventHandler<MouseEvent> {
        private final ViewModel viewModel;

        private StopButtonMouseEventHandler(ViewModel viewModel) {
            this.viewModel = requireNonNull(viewModel);
        }

        @Override
        public void handle(MouseEvent event) {
            viewModel.stopSelectedTorrent();
        }
    }

    private class AddButtonMouseEventHandler extends AddNewTorrentFileEventHandler<MouseEvent> {

        private AddButtonMouseEventHandler(ViewModel viewModel) {
            super(viewModel);
        }

        @Override
        protected Window getOwnerWindow() {
            return getScene().getWindow();
        }

        @Override
        protected boolean shouldHandle(MouseEvent event) {
            return event.getButton() == MouseButton.PRIMARY;
        }
    }
}
