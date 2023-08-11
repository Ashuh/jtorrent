package jtorrent.presentation.viewmodel;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.domain.manager.TorrentManager;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.presentation.model.UiPeer;
import jtorrent.presentation.model.UiTorrent;

public class ViewModel {

    private final TorrentManager torrentManager;

    private final ObservableList<UiTorrent> uiTorrents = FXCollections.observableList(new ArrayList<>());

    private final ObservableList<UiPeer> uiPeers = FXCollections.observableList(new ArrayList<>());

    private final Map<UiTorrent, Torrent> uiTorrentToTorrent = new HashMap<>();

    private Torrent selectedTorrent;

    public ViewModel(TorrentManager torrentManager) {
        this.torrentManager = requireNonNull(torrentManager);

        torrentManager.getTorrents().subscribe(event -> {
            switch (event.getEventType()) {
            case ADD:
                UiTorrent uiTorrent = UiTorrent.fromDomain(event.getItem());
                uiTorrentToTorrent.put(uiTorrent, event.getItem());
                uiTorrents.add(event.getIndex(), uiTorrent);
                break;
            case REMOVE:
                UiTorrent removed = uiTorrents.remove(event.getIndex());
                uiTorrentToTorrent.remove(removed);
                break;
            default:
                throw new AssertionError("Unknown event type: " + event.getEventType());
            }
        });
    }

    public void setTorrentSelected(UiTorrent uiTorrent) {
        Torrent torrent = uiTorrentToTorrent.get(uiTorrent);
        selectedTorrent = torrent;
        uiPeers.clear();

        torrent.getPeersObservable().subscribe(peers -> {
            uiPeers.clear();
            uiPeers.addAll(peers.stream().map(UiPeer::fromDomain).collect(Collectors.toList()));
        });
    }

    public ObservableList<UiTorrent> getTorrents() {
        return FXCollections.unmodifiableObservableList(uiTorrents);
    }

    public ObservableList<UiPeer> getPeers() {
        return FXCollections.unmodifiableObservableList(uiPeers);
    }

    public void startSelectedTorrent() {
        if (selectedTorrent == null) {
            return;
        }
        torrentManager.startTorrent(selectedTorrent);
    }

    public void stopSelectedTorrent() {
        if (selectedTorrent == null) {
            return;
        }
        torrentManager.stopTorrent(selectedTorrent);
    }
}
