package jtorrent.presentation.main.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.disposables.Disposable;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.domain.Client;
import jtorrent.domain.peer.model.Peer;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.main.model.UiPeer;

public class PeersTableViewModel {

    private final Client client;
    private final ObservableList<UiPeer> uiPeers = FXCollections.observableList(new ArrayList<>());
    private final Map<Peer, UiPeer> peerToUiPeer = new HashMap<>();

    private Torrent selectedTorrent;
    private Disposable selectedTorrentPeersSubscription;

    public PeersTableViewModel(Client client) {
        this.client = requireNonNull(client);
    }

    public void setSelectedTorrent(Torrent torrent) {
        if (torrent == selectedTorrent) {
            return;
        }

        selectedTorrent = torrent;

        reset();

        if (selectedTorrent == null) {
            return;
        }

        selectedTorrentPeersSubscription = subscribeToPeers(selectedTorrent);
    }

    private void reset() {
        if (selectedTorrentPeersSubscription != null) {
            selectedTorrentPeersSubscription.dispose();
            selectedTorrentPeersSubscription = null;
        }

        uiPeers.forEach(UiPeer::dispose);
        Platform.runLater(uiPeers::clear);
    }

    private Disposable subscribeToPeers(Torrent torrent) {
        return torrent.getPeersObservable().subscribe(event -> {
            switch (event.getType()) {
            case ADD:
                UiPeer uiPeer = UiPeer.fromDomain(event.getItem());
                peerToUiPeer.put(event.getItem(), uiPeer);
                Platform.runLater(() -> uiPeers.add(uiPeer));
                break;
            case REMOVE:
                UiPeer removed = peerToUiPeer.remove(event.getItem());
                Platform.runLater(() -> uiPeers.remove(removed));
                break;
            case CLEAR:
                Platform.runLater(uiPeers::clear);
                break;
            default:
                throw new AssertionError("Unknown event type: " + event.getType());
            }
        });
    }

    public ObservableList<UiPeer> getUiPeers() {
        return FXCollections.unmodifiableObservableList(uiPeers);
    }

    public void addPeerForSelectedTorrent(String ipPort) throws UnknownHostException {
        if (selectedTorrent != null) {
            PeerContactInfo peerContactInfo = PeerContactInfo.fromString(ipPort);
            client.addPeer(selectedTorrent, peerContactInfo);
        }
    }

    public boolean hasSelectedTorrent() {
        return selectedTorrent != null;
    }
}
