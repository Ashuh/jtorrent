package jtorrent.presentation.common.util;

import javafx.stage.FileChooser;

public class FileChooserUtil {

    private static final FileChooser.ExtensionFilter TORRENT_EXTENSION_FILTER =
            new FileChooser.ExtensionFilter("Torrents (*.torrent)", "*.torrent");

    private FileChooserUtil() {
    }

    public static FileChooser createTorrentFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(TORRENT_EXTENSION_FILTER);
        chooser.setTitle(title);
        return chooser;
    }
}
