package jtorrent.presentation.view;

import java.io.IOException;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import jtorrent.presentation.model.UiTorrentContents;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;

public class AddNewTorrentDialog extends Dialog<UiTorrentContents> {

    private final AddNewTorrentView addNewTorrentView;

    public AddNewTorrentDialog(UiTorrentContents torrentContents) {
        try {
            JTorrentFxmlLoader loader = new JTorrentFxmlLoader();
            DialogPane dialogPane = loader.load("AddNewTorrentView.fxml");
            addNewTorrentView = loader.getController();
            addNewTorrentView.setTorrentContents(torrentContents);
            setDialogPane(dialogPane);
            setTitle(torrentContents.getName() + " - Add New Torrent");
            setResultConverter(this::convertResult);
            setResizable(true);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private UiTorrentContents convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            return addNewTorrentView.getTorrentContents();
        } else {
            return null;
        }
    }
}
