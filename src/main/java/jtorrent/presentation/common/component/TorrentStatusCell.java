package jtorrent.presentation.common.component;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import jtorrent.presentation.main.model.UiTorrent;

public class TorrentStatusCell extends TableCell<UiTorrent, TorrentStatusCell.Status> {

    private final StackPane stackPane = new StackPane();
    private final ProgressBar progressBar = new ProgressBar();
    private final Text text = new Text();

    public TorrentStatusCell() {
        progressBar.setMaxWidth(Double.MAX_VALUE);
        stackPane.getChildren().addAll(progressBar, text);
    }

    @Override
    protected void updateItem(Status item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setGraphic(null);
        } else {
            progressBar.progressProperty().bind(item.progressProperty());
            text.textProperty().bind(item.stateProperty());
            setGraphic(stackPane);
        }
    }

    public static class Status {

        private final StringProperty state;
        private final DoubleProperty progress;

        public Status(StringProperty state, DoubleProperty progress) {
            this.state = requireNonNull(state);
            this.progress = requireNonNull(progress);
        }

        public ReadOnlyStringProperty stateProperty() {
            return state;
        }

        public ReadOnlyDoubleProperty progressProperty() {
            return progress;
        }
    }
}
