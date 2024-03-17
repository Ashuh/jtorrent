package jtorrent.presentation.main.util;

import java.io.IOException;
import java.util.Optional;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.stage.Window;
import jtorrent.data.torrent.model.BencodedTorrent;
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
            BencodedTorrent bencodedTorrent = loadTorrent(userInput.get());
            AddNewTorrentDialog dialog = new AddNewTorrentDialog(bencodedTorrent);
            dialog.initOwner(getOwnerWindow());
            Optional<AddNewTorrentDialog.Result> result = dialog.showAndWait();
            result.ifPresent(r -> addTorrent(bencodedTorrent, r));
        } catch (IOException e) {
            ExceptionAlert alert = new ExceptionAlert("Error", "Failed to load torrent", e);
            alert.showAndWait();
        }
    }

    protected abstract Optional<T> getUserInput();

    protected abstract BencodedTorrent loadTorrent(T userInput) throws IOException;

    protected abstract Window getOwnerWindow();

    protected abstract void addTorrent(BencodedTorrent bencodedTorrent, AddNewTorrentDialog.Result result);

    protected abstract boolean shouldHandle(E event);
}
