package jtorrent.presentation.view;

import java.io.IOException;
import java.util.Optional;

import javafx.event.Event;
import javafx.scene.control.TextInputDialog;
import jtorrent.presentation.model.UiTorrentContents;
import jtorrent.presentation.viewmodel.ViewModel;

public abstract class AddNewTorrentUrlEventHandler<E extends Event> extends AddNewTorrentEventHandler<String, E> {

    private static final String TITLE = "Add Torrent from URL";
    private static final String HEADER = "Please enter the location of the .torrent you want to open:";

    protected AddNewTorrentUrlEventHandler(ViewModel viewModel) {
        super(viewModel);
    }

    @Override
    protected Optional<String> getUserInput() {
        var dialog = new TextInputDialog();
        dialog.setTitle(TITLE);
        dialog.setHeaderText(HEADER);
        dialog.setGraphic(null);
        dialog.initOwner(getOwnerWindow());
        return dialog.showAndWait();
    }

    @Override
    protected UiTorrentContents getTorrentContents(String userInput) throws IOException {
        return viewModel.loadTorrentContents(userInput);
    }
}
