package jtorrent.presentation.main.util;

import java.util.Optional;

import javafx.event.Event;
import javafx.scene.control.TextInputDialog;

public abstract class AddNewTorrentUrlEventHandler<E extends Event> extends AddNewTorrentEventHandler<String, E> {

    private static final String TITLE = "Add Torrent from URL";
    private static final String HEADER = "Please enter the location of the .torrent you want to open:";

    @Override
    protected Optional<String> getUserInput() {
        var dialog = new TextInputDialog();
        dialog.setTitle(TITLE);
        dialog.setHeaderText(HEADER);
        dialog.setGraphic(null);
        dialog.initOwner(getOwnerWindow());
        return dialog.showAndWait();
    }
}
