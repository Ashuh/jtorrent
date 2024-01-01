package jtorrent.torrent.presentation.view;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import jtorrent.torrent.presentation.UiTorrentContents;

public class AddNewTorrentDialog extends Dialog<AddNewTorrentDialog.Options> {

    private final AddNewTorrentDialogPane dialogPane;

    public AddNewTorrentDialog(UiTorrentContents torrentContents) {
        this.dialogPane = new AddNewTorrentDialogPane(torrentContents);
        setTitle(torrentContents.getName() + " - Add New Torrent");
        setDialogPane(dialogPane);
        setResultConverter(this::convertResult);
        setResizable(true);
    }

    private Options convertResult(ButtonType dialogButton) {
        if (dialogButton == ButtonType.OK) {
            String name = dialogPane.getName();
            String saveDirectory = dialogPane.getSaveDirectory();
            return new Options(name, saveDirectory);
        } else {
            return null;
        }
    }

    public record Options(String name, String saveDirectory) {
    }
}
