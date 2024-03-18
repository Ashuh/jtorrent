package jtorrent.presentation.main.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.domain.Client;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.main.model.UiTorrent;

public class TorrentsTableViewModel {

    private static final System.Logger LOGGER = System.getLogger(TorrentsTableViewModel.class.getName());
    private static final String EXPLORER_EXE = "explorer.exe";

    private final Client client;
    private final ObservableList<UiTorrent> uiTorrents = FXCollections.observableList(new ArrayList<>());
    private final Map<UiTorrent, Sha1Hash> uiTorrentToInfoHash = new HashMap<>();
    private final Map<Sha1Hash, UiTorrent> infoHashToUiTorrent = new HashMap<>();
    private final Consumer<Torrent> torrentSelectedConsumer;
    private Torrent selectedTorrent;

    public TorrentsTableViewModel(Client client, Consumer<Torrent> torrentSelectedConsumer) {
        this.client = requireNonNull(client);
        this.torrentSelectedConsumer = requireNonNull(torrentSelectedConsumer);

        client.getTorrents().subscribe(event -> {
            Optional<Integer> indexOptional = event.getIndex();
            Sha1Hash infoHash;
            switch (event.getType()) {
            case ADD:
                UiTorrent uiTorrent = UiTorrent.fromDomain(event.getItem());
                infoHash = event.getItem().getInfoHash();
                uiTorrentToInfoHash.put(uiTorrent, infoHash);
                infoHashToUiTorrent.put(infoHash, uiTorrent);
                assert indexOptional.isPresent();
                Platform.runLater(() -> uiTorrents.add(indexOptional.get(), uiTorrent));
                break;
            case REMOVE:
                assert indexOptional.isPresent();
                infoHash = event.getItem().getInfoHash();
                UiTorrent removed = infoHashToUiTorrent.remove(infoHash);
                uiTorrentToInfoHash.remove(removed);
                removed.dispose();
                Platform.runLater(() -> uiTorrents.remove(removed));
                break;
            case CLEAR:
                uiTorrentToInfoHash.clear();
                infoHashToUiTorrent.clear();
                uiTorrents.forEach(UiTorrent::dispose);
                Platform.runLater(uiTorrents::clear);
                break;
            default:
                throw new AssertionError("Unknown event type: " + event.getType());
            }
        });
    }

    public void setTorrentSelected(UiTorrent uiTorrent) {
        Torrent torrent = getTorrentFromUiTorrent(uiTorrent);
        selectedTorrent = torrent;
        torrentSelectedConsumer.accept(torrent);
    }

    private Torrent getTorrentFromUiTorrent(UiTorrent uiTorrent) {
        Sha1Hash infoHash = uiTorrentToInfoHash.get(uiTorrent);
        return client.getTorrent(infoHash);
    }

    public boolean hasSelectedTorrent() {
        return selectedTorrent != null;
    }

    public Optional<Torrent> getSelectedTorrent() {
        return Optional.ofNullable(selectedTorrent);
    }

    public void showTorrentInFileExplorer(UiTorrent uiTorrent) {
        Torrent torrent = getTorrentFromUiTorrent(uiTorrent);

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
