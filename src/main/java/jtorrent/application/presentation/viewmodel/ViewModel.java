package jtorrent.application.presentation.viewmodel;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.reactivex.rxjava3.disposables.Disposable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.application.domain.Client;
import jtorrent.peer.domain.model.Peer;
import jtorrent.peer.presentation.UiPeer;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.torrent.presentation.UiTorrent;

public class ViewModel {

    private final Client client;
    private final ObservableList<UiTorrent> uiTorrents = FXCollections.observableList(new ArrayList<>());
    private final ObservableList<UiPeer> uiPeers = FXCollections.observableList(new ArrayList<>());
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
        client.startTorrent(selectedTorrent);
    }

    public void stopSelectedTorrent() {
        if (selectedTorrent == null) {
            return;
        }
        client.stopTorrent(selectedTorrent);
    }
}
