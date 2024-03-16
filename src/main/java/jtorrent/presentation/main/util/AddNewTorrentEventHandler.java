package jtorrent.presentation.main.util;

import java.io.IOException;
import java.util.Optional;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.stage.Window;
import jtorrent.presentation.addnewtorrent.view.AddNewTorrentDialog;
import jtorrent.presentation.common.model.UiTorrentContents;
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
            UiTorrentContents torrentContents = getTorrentContents(userInput.get());
            AddNewTorrentDialog dialog = new AddNewTorrentDialog(torrentContents);
            dialog.initOwner(getOwnerWindow());
            Optional<UiTorrentContents> torrentContentsResult = dialog.showAndWait();
            torrentContentsResult.ifPresent(this::addTorrent);
        } catch (IOException e) {
            ExceptionAlert alert = new ExceptionAlert("Error", "Failed to load torrent", e);
            alert.showAndWait();
        }
    }

    protected abstract Optional<T> getUserInput();

    protected abstract UiTorrentContents getTorrentContents(T userInput) throws IOException;

    protected abstract Window getOwnerWindow();

    protected abstract void addTorrent(UiTorrentContents torrentContents);

    protected abstract boolean shouldHandle(E event);
}
