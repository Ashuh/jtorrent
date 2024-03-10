package jtorrent.presentation.view;

import static jtorrent.presentation.util.FileChooserUtil.createTorrentFileChooser;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.event.Event;
import javafx.stage.FileChooser;
import jtorrent.presentation.model.UiTorrentContents;
import jtorrent.presentation.viewmodel.ViewModel;

public abstract class AddNewTorrentFileEventHandler<E extends Event> extends AddNewTorrentEventHandler<File, E> {

    private static final String TITLE = "Select a .torrent to open";

    protected AddNewTorrentFileEventHandler(ViewModel viewModel) {
        super(viewModel);
    }

    @Override
    protected Optional<File> getUserInput() {
        FileChooser chooser = createTorrentFileChooser(TITLE);
        return Optional.ofNullable(chooser.showOpenDialog(getOwnerWindow()));
    }

    @Override
    protected UiTorrentContents getTorrentContents(File file) throws IOException {
        return viewModel.loadTorrentContents(file);
    }
}
