package jtorrent.presentation.main.view;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import jtorrent.presentation.common.component.TorrentStatusCell;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;
import jtorrent.presentation.main.model.UiTorrent;
import jtorrent.presentation.main.viewmodel.TorrentsTableViewModel;

public class TorrentsTableView extends TableView<UiTorrent> {

    private final ObjectProperty<TorrentsTableViewModel> viewModel = new SimpleObjectProperty<>();
    @FXML
    private TableColumn<UiTorrent, String> name;
    @FXML
    private TableColumn<UiTorrent, String> size;
    @FXML
    private TableColumn<UiTorrent, TorrentStatusCell.Status> status;
    @FXML
    private TableColumn<UiTorrent, String> downSpeed;
    @FXML
    private TableColumn<UiTorrent, String> upSpeed;
    @FXML
    private TableColumn<UiTorrent, String> eta;
    @FXML
    private TableColumn<UiTorrent, String> saveDirectory;

    public TorrentsTableView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public ObjectProperty<TorrentsTableViewModel> viewModelProperty() {
        return viewModel;
    }

    @FXML
    public void initialize() {
        viewModel.addListener(new ViewModelChangeListener());

        itemsProperty().bind(viewModel
                .map(TorrentsTableViewModel::getUiTorrents)
                .map(SortedList::new)
                .map(sortedList -> {
                    sortedList.comparatorProperty().bind(comparatorProperty());
                    return sortedList;
                })
        );

        name.setCellValueFactory(param -> param.getValue().nameProperty());
        size.setCellValueFactory(param -> param.getValue().sizeProperty());
        status.setCellValueFactory(param -> param.getValue().statusProperty());
        status.setCellFactory(param -> new TorrentStatusCell());
        downSpeed.setCellValueFactory(param -> param.getValue().downSpeedProperty());
        upSpeed.setCellValueFactory(param -> param.getValue().upSpeedProperty());
        eta.setCellValueFactory(param -> param.getValue().etaProperty());
        saveDirectory.setCellValueFactory(param -> param.getValue().saveDirectoryProperty());
        setRowFactory(param -> new TorrentTableRow());
    }

    private class TorrentTableRow extends TableRow<UiTorrent> {

        public TorrentTableRow() {
            onMouseClickedProperty().bind(viewModel.map(MouseEventHandler::new));
        }

        private class MouseEventHandler implements EventHandler<MouseEvent> {
            private final TorrentsTableViewModel viewModel;

            private MouseEventHandler(TorrentsTableViewModel viewModel) {
                this.viewModel = requireNonNull(viewModel);
            }

            @Override
            public void handle(MouseEvent event) {
                if (isEmpty()) {
                    getSelectionModel().clearSelection();
                    return;
                }
                if (event.getClickCount() == 2) {
                    UiTorrent torrent = getItem();
                    viewModel.showTorrentInFileExplorer(torrent);
                }
            }
        }
    }

    private class ViewModelChangeListener implements ChangeListener<TorrentsTableViewModel> {
        private ChangeListener<UiTorrent> listener;

        @Override
        public void changed(ObservableValue<? extends TorrentsTableViewModel> observable,
                TorrentsTableViewModel oldValue, TorrentsTableViewModel newValue) {
            if (oldValue != null) {
                getSelectionModel().selectedItemProperty().removeListener(listener);
            }

            if (newValue != null) {
                listener = new TorrentChangeListener(newValue);
                getSelectionModel().selectedItemProperty().addListener(listener);
            } else {
                listener = null;
            }
        }

        private static class TorrentChangeListener implements ChangeListener<UiTorrent> {
            private final TorrentsTableViewModel viewModel;

            private TorrentChangeListener(TorrentsTableViewModel viewModel) {
                this.viewModel = requireNonNull(viewModel);
            }

            @Override
            public void changed(ObservableValue<? extends UiTorrent> observable, UiTorrent oldValue,
                    UiTorrent newValue) {
                viewModel.setTorrentSelected(newValue);
            }
        }
    }
}
