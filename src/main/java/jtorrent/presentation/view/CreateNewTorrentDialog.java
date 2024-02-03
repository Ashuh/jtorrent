package jtorrent.presentation.view;

import java.io.IOException;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import jtorrent.presentation.model.UiNewTorrent;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;

public class CreateNewTorrentDialog extends Dialog<UiNewTorrent> {

    private final CreateNewTorrentView createNewTorrentView;

    public CreateNewTorrentDialog() {
        try {
            JTorrentFxmlLoader loader = new JTorrentFxmlLoader();
            DialogPane dialogPane = loader.load("CreateNewTorrentView.fxml");
            createNewTorrentView = loader.getController();
            setDialogPane(dialogPane);
            setTitle("Create New Torrent");
            setResultConverter(this::convertResult);
            setResizable(false);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private UiNewTorrent convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            return createNewTorrentView.getNewTorrent();
        } else {
            return null;
        }
    }
}
