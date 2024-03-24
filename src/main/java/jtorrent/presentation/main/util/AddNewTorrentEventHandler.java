package jtorrent.presentation.main.util;

import java.io.IOException;
import java.util.Optional;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.stage.Window;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.presentation.addnewtorrent.view.AddNewTorrentDialog;
import jtorrent.presentation.exception.view.ExceptionAlert;

public abstract class AddNewTorrentEventHandler<T, E extends Event> implements EventHandler<E> {

    @Override
    public final void handle(E event) {
        if (shouldHandle(event)) {
            handle();
        }
    }

    private void handle() {
        Optional<T> userInput = getUserInput();

        if (userInput.isEmpty()) {
            return;
        }

        try {
            TorrentMetadata torrentMetadata = loadTorrent(userInput.get());
            AddNewTorrentDialog dialog = new AddNewTorrentDialog(torrentMetadata);
            dialog.initOwner(getOwnerWindow());
            Optional<AddNewTorrentDialog.Result> result = dialog.showAndWait();
            result.ifPresent(r -> addTorrent(torrentMetadata, r));
        } catch (IOException e) {
            ExceptionAlert alert = new ExceptionAlert("Error", "Failed to load torrent", e);
            alert.showAndWait();
        }
    }

    protected abstract Optional<T> getUserInput();

    protected abstract TorrentMetadata loadTorrent(T userInput) throws IOException;

    protected abstract Window getOwnerWindow();

    protected abstract void addTorrent(TorrentMetadata torrentMetadata, AddNewTorrentDialog.Result result);

    protected abstract boolean shouldHandle(E event);
}
