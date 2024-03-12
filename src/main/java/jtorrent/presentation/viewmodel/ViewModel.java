package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.domain.Client;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.model.UiChartData;
import jtorrent.presentation.model.UiFileInfo;
import jtorrent.presentation.model.UiTorrentContents;
import jtorrent.presentation.model.UiTorrentControlsState;
import jtorrent.presentation.model.UiTorrentInfo;

public class ViewModel {

    private final Client client;
    private final ObjectProperty<UiChartData> chartData = new SimpleObjectProperty<>();
    private final ObjectProperty<ObservableList<UiFileInfo>> uiFileInfos = new SimpleObjectProperty<>();
    private final ObjectProperty<UiTorrentInfo> uiTorrentInfo = new SimpleObjectProperty<>(null);
    private final ObjectProperty<UiTorrentControlsState> torrentControlsState = new SimpleObjectProperty<>();
    private final BehaviorSubject<Optional<Torrent>> selectedTorrentSubject = BehaviorSubject
            .createDefault(Optional.empty());
    private final TorrentsTableViewModel torrentsTableViewModel;
    private final PeersTableViewModel peersTableViewModel;

    public ViewModel(Client client) {
        this.client = requireNonNull(client);
        torrentControlsState.set(UiTorrentControlsState.build(selectedTorrentSubject));
        chartData.set(UiChartData.build(client));
        torrentsTableViewModel = new TorrentsTableViewModel(client, this::onTorrentSelected);
        peersTableViewModel = new PeersTableViewModel(client);
    }

    public TorrentsTableViewModel getTorrentsTableViewModel() {
        return torrentsTableViewModel;
    }

    public PeersTableViewModel getPeersTableViewModel() {
        return peersTableViewModel;
    }

    private void setSelectedTorrent(Torrent selectedTorrent) {
        selectedTorrentSubject.onNext(Optional.ofNullable(selectedTorrent));
    }

    private void onTorrentSelected(Torrent torrent) {
        peersTableViewModel.setSelectedTorrent(torrent);

        if (uiFileInfos.get() != null) {
            uiFileInfos.get().forEach(UiFileInfo::dispose);
        }

        if (uiTorrentInfo.get() != null) {
            uiTorrentInfo.get().dispose();
        }

        if (torrent == null) {
            setSelectedTorrent(null);
            uiFileInfos.set(null);
            uiTorrentInfo.set(null);
            return;
        }

        setSelectedTorrent(torrent);

        List<UiFileInfo> selectedUiFilesInfos = torrent.getFilesWithInfo().stream()
                .map(UiFileInfo::fromDomain)
                .toList();
        Platform.runLater(() -> uiFileInfos.set(FXCollections.observableList(selectedUiFilesInfos)));

        UiTorrentInfo selectedUiTorrentInfo = UiTorrentInfo.fromDomain(torrent);
        Platform.runLater(() -> uiTorrentInfo.set(selectedUiTorrentInfo));
    }

    public void startSelectedTorrent() {
        getSelectedTorrent().ifPresent(client::startTorrent);
    }

    public void stopSelectedTorrent() {
        getSelectedTorrent().ifPresent(client::stopTorrent);
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
        getSelectedTorrent().ifPresent(client::removeTorrent);
    }

    public Optional<Torrent> getSelectedTorrent() {
        return torrentsTableViewModel.getSelectedTorrent();
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
