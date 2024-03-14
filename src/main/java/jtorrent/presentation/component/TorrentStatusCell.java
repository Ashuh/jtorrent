package jtorrent.presentation.component;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import jtorrent.presentation.model.UiTorrent;
import jtorrent.presentation.model.UiTorrentStatus;

public class TorrentStatusCell extends TableCell<UiTorrent, UiTorrentStatus> {

    private final StackPane stackPane = new StackPane();
    private final ProgressBar progressBar = new ProgressBar();
    private final Text text = new Text();

    public TorrentStatusCell() {
        progressBar.setMaxWidth(Double.MAX_VALUE);
        stackPane.getChildren().addAll(progressBar, text);
    }

    @Override
    protected void updateItem(UiTorrentStatus item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setGraphic(null);
        } else {
            progressBar.progressProperty().bind(item.progressProperty());
            text.textProperty().bind(item.stateProperty());
            setGraphic(stackPane);
        }
    }
}
