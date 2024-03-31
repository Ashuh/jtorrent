package jtorrent.presentation.main.util;

import static jtorrent.presentation.common.util.FileChooserUtil.createTorrentFileChooser;

import java.io.File;
import java.util.Optional;

import javafx.event.Event;
import javafx.stage.FileChooser;

public abstract class AddNewTorrentFileEventHandler<E extends Event> extends AddNewTorrentEventHandler<File, E> {

    private static final String TITLE = "Select a .torrent to open";

    @Override
    protected Optional<File> getUserInput() {
        FileChooser chooser = createTorrentFileChooser(TITLE);
        return Optional.ofNullable(chooser.showOpenDialog(getOwnerWindow()));
    }
}
