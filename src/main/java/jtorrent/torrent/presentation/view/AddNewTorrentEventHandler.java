package jtorrent.torrent.presentation.view;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import jtorrent.application.presentation.viewmodel.ViewModel;
import jtorrent.common.presentation.ExceptionAlert;
import jtorrent.torrent.presentation.UiTorrentContents;

public abstract class AddNewTorrentEventHandler<E extends Event> implements EventHandler<E> {

    private static final String DESCRIPTION = "Torrents (*.torrent)";
    private static final String EXTENSION = "*.torrent";
    private static final String TITLE = "Select a .torrent to open";

    private final ViewModel viewModel;

    protected AddNewTorrentEventHandler(ViewModel viewModel) {
        this.viewModel = requireNonNull(viewModel);
    }

    @Override
    public void handle(E event) {
        if (shouldHandle(event)) {
            handle();
        }
    }

    private void handle() {
        FileChooser chooser = createFileChooser();
        File file = chooser.showOpenDialog(getOwnerWindow());

        if (file != null) {
            try {
                UiTorrentContents torrentContents = viewModel.loadTorrentContents(file);
                AddNewTorrentDialog dialog = new AddNewTorrentDialog(torrentContents);
                Optional<AddNewTorrentDialog.Options> options = dialog.showAndWait();
                if (options.isPresent()) {
                    viewModel.addTorrentFromFile(file, options.get());
                }
            } catch (IOException e) {
                ExceptionAlert alert = new ExceptionAlert("Error", "Failed to load torrent", e);
                alert.showAndWait();
            }
        }
    }

    private FileChooser createFileChooser() {
        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(DESCRIPTION, EXTENSION);
        chooser.getExtensionFilters().add(extFilter);
        chooser.setTitle(TITLE);
        return chooser;
    }

    protected abstract Window getOwnerWindow();

    protected abstract boolean shouldHandle(E event);
}