package jtorrent.presentation.view;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.event.Event;
import javafx.stage.FileChooser;
import jtorrent.presentation.model.UiTorrentContents;
import jtorrent.presentation.viewmodel.ViewModel;

public abstract class AddNewTorrentFileEventHandler<E extends Event> extends AddNewTorrentEventHandler<File, E> {

    private static final String DESCRIPTION = "Torrents (*.torrent)";
    private static final String EXTENSION = "*.torrent";
    private static final String TITLE = "Select a .torrent to open";

    protected AddNewTorrentFileEventHandler(ViewModel viewModel) {
        super(viewModel);
    }

    @Override
    protected Optional<File> getUserInput() {
        FileChooser chooser = createFileChooser();
        return Optional.ofNullable(chooser.showOpenDialog(getOwnerWindow()));
    }

    @Override
    protected UiTorrentContents getTorrentContents(File file) throws IOException {
        return viewModel.loadTorrentContents(file);
    }

    private FileChooser createFileChooser() {
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(DESCRIPTION, EXTENSION);
        chooser.getExtensionFilters().add(extFilter);
        chooser.setTitle(TITLE);
        return chooser;
    }
}
