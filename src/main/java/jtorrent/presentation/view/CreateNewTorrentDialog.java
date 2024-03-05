package jtorrent.presentation.view;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import jtorrent.presentation.model.UiNewTorrent;

public class CreateNewTorrentDialog extends Dialog<UiNewTorrent> {

    private final CreateNewTorrentView createNewTorrentView;

    public CreateNewTorrentDialog() {
        createNewTorrentView = new CreateNewTorrentView();
        setDialogPane(createNewTorrentView);
        setTitle("Create New Torrent");
        setResultConverter(this::convertResult);
        setResizable(false);
    }

    private UiNewTorrent convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            return createNewTorrentView.getNewTorrent();
        } else {
            return null;
        }
    }
}
