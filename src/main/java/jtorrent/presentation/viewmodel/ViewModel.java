package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.domain.Client;
import jtorrent.domain.peer.model.Peer;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.model.UiChartData;
import jtorrent.presentation.model.UiFileInfo;
import jtorrent.presentation.model.UiPeer;
import jtorrent.presentation.model.UiTorrentContents;
import jtorrent.presentation.model.UiTorrentControlsState;
import jtorrent.presentation.model.UiTorrentInfo;

public class ViewModel {

    private final Client client;
    private final ObservableList<UiPeer> uiPeers = FXCollections.observableList(new ArrayList<>());
    private final ObjectProperty<UiChartData> chartData = new SimpleObjectProperty<>();
    private final ObjectProperty<ObservableList<UiFileInfo>> uiFileInfos = new SimpleObjectProperty<>();
    private final ObjectProperty<UiTorrentInfo> uiTorrentInfo = new SimpleObjectProperty<>(null);
    private final ObjectProperty<UiTorrentControlsState> torrentControlsState = new SimpleObjectProperty<>();
    private final Map<Peer, UiPeer> peerToUiPeer = new HashMap<>();
    private final BehaviorSubject<Optional<Torrent>> selectedTorrentSubject = BehaviorSubject
            .createDefault(Optional.empty());
    private Disposable selectedTorrentPeersSubscription;
    private final TorrentsTableViewModel torrentsTableViewModel;

    public ViewModel(Client client) {
        this.client = requireNonNull(client);
        torrentControlsState.set(UiTorrentControlsState.build(selectedTorrentSubject));
        chartData.set(UiChartData.build(client));
        torrentsTableViewModel = new TorrentsTableViewModel(client, this::onTorrentSelected);
    }

    public TorrentsTableViewModel getTorrentsTableViewModel() {
        return torrentsTableViewModel;
    }

    private void setSelectedTorrent(Torrent selectedTorrent) {
        selectedTorrentSubject.onNext(Optional.ofNullable(selectedTorrent));
    }

    private void onTorrentSelected(Torrent torrent) {
        if (selectedTorrentPeersSubscription != null) {
            selectedTorrentPeersSubscription.dispose();
            selectedTorrentPeersSubscription = null;
        }

        if (uiFileInfos.get() != null) {
            uiFileInfos.get().forEach(UiFileInfo::dispose);
        }

        if (uiTorrentInfo.get() != null) {
            uiTorrentInfo.get().dispose();
        }

        uiPeers.forEach(UiPeer::dispose);
        uiPeers.clear();

        if (torrent == null) {
            setSelectedTorrent(null);
            uiFileInfos.set(null);
            uiTorrentInfo.set(null);
            return;
        }

        setSelectedTorrent(torrent);

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
        Platform.runLater(() -> uiFileInfos.set(FXCollections.observableList(selectedUiFilesInfos)));

        UiTorrentInfo selectedUiTorrentInfo = UiTorrentInfo.fromDomain(torrent);
        Platform.runLater(() -> uiTorrentInfo.set(selectedUiTorrentInfo));
    }

    public boolean hasSelectedTorrent() {
        return torrentsTableViewModel.hasSelectedTorrent();
    }

    public void addPeerForSelectedTorrent(String ipPort) throws UnknownHostException {
        Optional<Torrent> selectedTorrent = torrentsTableViewModel.getSelectedTorrent();
        if (selectedTorrent.isPresent()) {
            PeerContactInfo peerContactInfo = PeerContactInfo.fromString(ipPort);
            client.addPeer(selectedTorrent.get(), peerContactInfo);
        }
    }

    public void startSelectedTorrent() {
        torrentsTableViewModel.getSelectedTorrent().ifPresent(client::startTorrent);
    }

    public void stopSelectedTorrent() {
        torrentsTableViewModel.getSelectedTorrent().ifPresent(client::stopTorrent);
    }

    public UiTorrentContents loadTorrentContents(File file) throws IOException {
        Torrent torrent = client.loadTorrent(file);
        return UiTorrentContents.forTorrent(torrent);
    }

    public UiTorrentContents loadTorrentContents(String urlString) throws IOException {
        URL url = new URL(urlString);
        Torrent torrent = client.loadTorrent(url);
        return UiTorrentContents.forTorrent(torrent);
    }

    public void addTorrent(UiTorrentContents uiTorrentContents) throws IOException {
        Torrent torrent = uiTorrentContents.getTorrent();
        client.addTorrent(torrent);
    }

    public void removeSelectedTorrent() {
        torrentsTableViewModel.getSelectedTorrent().ifPresent(client::removeTorrent);
    }

    public void createNewTorrent(File savePath, File source, String trackerUrls, String comment, int pieceSize)
            throws IOException {
        List<List<String>> trackerTiers = new ArrayList<>();

        for (String tier : trackerUrls.split("\n\n")) {
            List<String> trackers = Arrays.asList(tier.split("\n"));
            trackerTiers.add(trackers);
        }

        client.createNewTorrent(savePath.toPath(), source.toPath(), trackerTiers, comment, pieceSize);
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

    public ReadOnlyObjectProperty<UiChartData> chartDataProperty() {
        return chartData;
    }

    public ReadOnlyObjectProperty<UiTorrentControlsState> torrentControlsStateProperty() {
        return torrentControlsState;
    }
}
