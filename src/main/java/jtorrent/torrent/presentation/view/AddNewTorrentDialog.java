package jtorrent.torrent.presentation.view;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import jtorrent.application.presentation.UiManager;
import jtorrent.torrent.presentation.UiTorrentContents;

public class AddNewTorrentDialog extends Dialog<AddNewTorrentDialog.Options> {

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

    private Options convertResult(ButtonType dialogButton) {
        if (dialogButton == ButtonType.OK) {
            String name = addNewTorrentView.getName();
            String saveDirectory = addNewTorrentView.getSaveDirectory();
            return new Options(name, saveDirectory);
        } else {
            return null;
        }
    }

    public record Options(String name, String saveDirectory) {
    }
}