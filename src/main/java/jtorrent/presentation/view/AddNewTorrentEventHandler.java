package jtorrent.presentation.view;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.util.Optional;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.stage.Window;
import jtorrent.presentation.model.UiTorrentContents;
import jtorrent.presentation.viewmodel.ViewModel;

public abstract class AddNewTorrentEventHandler<T, E extends Event> implements EventHandler<E> {

    protected final ViewModel viewModel;

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
        Optional<T> userInput = getUserInput();

        if (userInput.isEmpty()) {
            return;
        }

        try {
            UiTorrentContents torrentContents = getTorrentContents(userInput.get());
            AddNewTorrentDialog dialog = new AddNewTorrentDialog(torrentContents);
            dialog.initOwner(getOwnerWindow());
            Optional<UiTorrentContents> torrentContentsResult = dialog.showAndWait();
            if (torrentContentsResult.isPresent()) {
                viewModel.addTorrent(torrentContentsResult.get());
            }
        } catch (IOException e) {
            ExceptionAlert alert = new ExceptionAlert("Error", "Failed to load torrent", e);
            alert.showAndWait();
        }
    }

    protected abstract Optional<T> getUserInput();

    protected abstract UiTorrentContents getTorrentContents(T userInput) throws IOException;

    protected abstract Window getOwnerWindow();

    protected abstract boolean shouldHandle(E event);
}
