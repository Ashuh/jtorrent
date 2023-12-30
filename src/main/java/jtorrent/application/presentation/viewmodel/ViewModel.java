package jtorrent.application.presentation.viewmodel;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.reactivex.rxjava3.disposables.Disposable;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.application.domain.Client;
import jtorrent.peer.domain.model.Peer;
import jtorrent.peer.presentation.UiPeer;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.torrent.presentation.UiFileInfo;
import jtorrent.torrent.presentation.UiTorrent;
import jtorrent.torrent.presentation.UiTorrentInfo;

public class ViewModel {

    private static final Logger LOGGER = System.getLogger(ViewModel.class.getName());
    private static final String EXPLORER_EXE = "explorer.exe";

    private final Client client;
    private final ObservableList<UiTorrent> uiTorrents = FXCollections.observableList(new ArrayList<>());
    private final ObservableList<UiPeer> uiPeers = FXCollections.observableList(new ArrayList<>());
    private final ObjectProperty<ObservableList<UiFileInfo>> uiFileInfos = new SimpleObjectProperty<>();
    private final ObjectProperty<UiTorrentInfo> uiTorrentInfo = new SimpleObjectProperty<>(null);
    private final Map<UiTorrent, Torrent> uiTorrentToTorrent = new HashMap<>();
    private final Map<Peer, UiPeer> peerToUiPeer = new HashMap<>();

    private Torrent selectedTorrent;
    private Disposable selectedTorrentPeersSubscription;

    public ViewModel(Client client) {
        this.client = requireNonNull(client);

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
        if (selectedTorrentPeersSubscription != null) {
            selectedTorrentPeersSubscription.dispose();
        }

        uiPeers.clear();

        if (uiTorrent == null) {
            selectedTorrent = null;
            selectedTorrentPeersSubscription = null;
            return;
        }

        Torrent torrent = uiTorrentToTorrent.get(uiTorrent);
        selectedTorrent = torrent;

        selectedTorrentPeersSubscription = torrent.getPeersObservable().subscribe(event -> {
            switch (event.getType()) {
            case ADD:
                UiPeer uiPeer = UiPeer.fromDomain(event.getItem());
                peerToUiPeer.put(event.getItem(), uiPeer);
                uiPeers.add(uiPeer);
                break;
            case REMOVE:
                UiPeer removed = peerToUiPeer.remove(event.getItem());
                uiPeers.remove(removed);
                break;
            case CLEAR:
                uiPeers.clear();
                break;
            default:
                throw new AssertionError("Unknown event type: " + event.getType());
            }
        });

        List<UiFileInfo> selectedUiFilesInfos = torrent.getFilesWithInfo().stream()
                .map(UiFileInfo::fromDomain)
                .toList();
        Platform.runLater(() -> {
            if (uiFileInfos.get() != null) {
                uiFileInfos.get().forEach(UiFileInfo::dispose);
            }
            uiFileInfos.set(FXCollections.observableList(selectedUiFilesInfos));
        });

        UiTorrentInfo selectedUiTorrentInfo = UiTorrentInfo.fromDomain(torrent);
        if (uiTorrentInfo.get() != null) {
            uiTorrentInfo.get().dispose();
        }
        Platform.runLater(() -> uiTorrentInfo.set(selectedUiTorrentInfo));
    }

    public void startSelectedTorrent() {
        if (selectedTorrent == null) {
            return;
        }
        client.startTorrent(selectedTorrent);
    }

    public void stopSelectedTorrent() {
        if (selectedTorrent == null) {
            return;
        }
        client.stopTorrent(selectedTorrent);
    }

    public void showTorrentInFileExplorer(UiTorrent uiTorrent) {
        Torrent torrent = uiTorrentToTorrent.get(uiTorrent);

        // only works on windows. Doing this because Desktop::browseFileDirectory doesn't work on Windows 10
        final String command = EXPLORER_EXE + " /SELECT,\"" + torrent.getSaveDirectory().toAbsolutePath() + "\"";
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Failed to open file explorer", e);
        }
    }

    public ObservableList<UiTorrent> getTorrents() {
        return FXCollections.unmodifiableObservableList(uiTorrents);
    }

    public ObservableList<UiPeer> getPeers() {
        return FXCollections.unmodifiableObservableList(uiPeers);
    }

    public ObjectProperty<ObservableList<UiFileInfo>> getFileInfos() {
        return uiFileInfos;
    }

    public ObjectProperty<UiTorrentInfo> getTorrentInfo() {
        return uiTorrentInfo;
    }
}
