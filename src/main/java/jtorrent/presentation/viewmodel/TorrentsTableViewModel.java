package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.domain.Client;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.model.UiTorrent;

public class TorrentsTableViewModel {

    private static final System.Logger LOGGER = System.getLogger(TorrentsTableViewModel.class.getName());
    private static final String EXPLORER_EXE = "explorer.exe";

    private final ObservableList<UiTorrent> uiTorrents = FXCollections.observableList(new ArrayList<>());
    private final Map<UiTorrent, Torrent> uiTorrentToTorrent = new HashMap<>();

    private final Consumer<Torrent> torrentSelectedConsumer;
    private Torrent selectedTorrent;

    public TorrentsTableViewModel(Client client, Consumer<Torrent> torrentSelectedConsumer) {
        this.torrentSelectedConsumer = requireNonNull(torrentSelectedConsumer);

        client.getTorrents().subscribe(event -> {
            Optional<Integer> indexOptional = event.getIndex();
            switch (event.getType()) {
            case ADD:
                UiTorrent uiTorrent = UiTorrent.fromDomain(event.getItem());
                uiTorrentToTorrent.put(uiTorrent, event.getItem());
                assert indexOptional.isPresent();
                uiTorrents.add(indexOptional.get(), uiTorrent);
                break;
            case REMOVE:
                assert indexOptional.isPresent();
                UiTorrent removed = uiTorrents.remove(indexOptional.get().intValue());
                removed.dispose();
                uiTorrentToTorrent.remove(removed);
                break;
            case CLEAR:
                uiTorrents.clear();
                break;
            default:
                throw new AssertionError("Unknown event type: " + event.getType());
            }
        });
    }

    public void setTorrentSelected(UiTorrent uiTorrent) {
        Torrent torrent = uiTorrentToTorrent.get(uiTorrent);
        selectedTorrent = torrent;
        torrentSelectedConsumer.accept(torrent);
    }

    public boolean hasSelectedTorrent() {
        return selectedTorrent != null;
    }

    public Optional<Torrent> getSelectedTorrent() {
        return Optional.ofNullable(selectedTorrent);
    }

    public void showTorrentInFileExplorer(UiTorrent uiTorrent) {
        Torrent torrent = uiTorrentToTorrent.get(uiTorrent);

        // only works on windows. Doing this because Desktop::browseFileDirectory doesn't work on Windows 10
        final String command = EXPLORER_EXE + " /SELECT,\"" + torrent.getSaveAsPath().toAbsolutePath() + "\"";
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Failed to open file explorer", e);
        }
    }

    public ObservableList<UiTorrent> getUiTorrents() {
        return FXCollections.unmodifiableObservableList(uiTorrents);
    }
}
