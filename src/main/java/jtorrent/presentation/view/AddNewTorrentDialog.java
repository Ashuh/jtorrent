package jtorrent.presentation.view;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import jtorrent.presentation.UiManager;
import jtorrent.presentation.model.UiTorrentContents;

public class AddNewTorrentDialog extends Dialog<UiTorrentContents> {

    private final AddNewTorrentView addNewTorrentView;

    public AddNewTorrentDialog(UiTorrentContents torrentContents) {
        FXMLLoader fxmlLoader = new FXMLLoader(UiManager.class.getResource("fxml/AddNewTorrentDialog.fxml"));
        try {
            DialogPane dialogPane = fxmlLoader.load();
            addNewTorrentView = fxmlLoader.getController();
            addNewTorrentView.setTorrentContents(torrentContents);
            setDialogPane(dialogPane);
            setTitle(torrentContents.getName() + " - Add New Torrent");
            setResultConverter(this::convertResult);
            setResizable(true);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private UiTorrentContents convertResult(ButtonType dialogButton) {
        if (dialogButton == ButtonType.OK) {
            return addNewTorrentView.getTorrentContents();
        } else {
            return null;
        }
    }
}
