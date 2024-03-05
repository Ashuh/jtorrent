package jtorrent.presentation.view;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import jtorrent.presentation.model.UiTorrentContents;

public class AddNewTorrentDialog extends Dialog<UiTorrentContents> {

    private final AddNewTorrentView addNewTorrentView;

    public AddNewTorrentDialog(UiTorrentContents torrentContents) {
        addNewTorrentView = new AddNewTorrentView();
        addNewTorrentView.setTorrentContents(torrentContents);
        setDialogPane(addNewTorrentView);
        setTitle(torrentContents.getName() + " - Add New Torrent");
        setResultConverter(this::convertResult);
        setResizable(true);
    }

    private UiTorrentContents convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            return addNewTorrentView.getTorrentContents();
        } else {
            return null;
        }
    }
}
