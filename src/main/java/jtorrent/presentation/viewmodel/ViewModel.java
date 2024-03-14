package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

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
import jtorrent.presentation.model.UiTorrentInfo;

public class ViewModel {

    private final Client client;
    private final ObjectProperty<UiChartData> chartData = new SimpleObjectProperty<>();
    private final ObjectProperty<ObservableList<UiFileInfo>> uiFileInfos = new SimpleObjectProperty<>();
    private final ObjectProperty<UiTorrentInfo> uiTorrentInfo = new SimpleObjectProperty<>(null);
    private final TorrentControlsViewModel torrentControlsViewModel;
    private final TorrentsTableViewModel torrentsTableViewModel;
    private final PeersTableViewModel peersTableViewModel;

    public ViewModel(Client client) {
        this.client = requireNonNull(client);
        chartData.set(UiChartData.build(client));
        torrentControlsViewModel = new TorrentControlsViewModel(client);
        torrentsTableViewModel = new TorrentsTableViewModel(client, this::onTorrentSelected);
        peersTableViewModel = new PeersTableViewModel(client);
    }

    public TorrentControlsViewModel getTorrentControlsViewModel() {
        return torrentControlsViewModel;
    }

    public TorrentsTableViewModel getTorrentsTableViewModel() {
        return torrentsTableViewModel;
    }

    public PeersTableViewModel getPeersTableViewModel() {
        return peersTableViewModel;
    }

    private void onTorrentSelected(Torrent torrent) {
        torrentControlsViewModel.setSelectedTorrent(torrent);
        peersTableViewModel.setSelectedTorrent(torrent);

        if (uiFileInfos.get() != null) {
            uiFileInfos.get().forEach(UiFileInfo::dispose);
        }

        if (uiTorrentInfo.get() != null) {
            uiTorrentInfo.get().dispose();
        }

        if (torrent == null) {
            uiFileInfos.set(null);
            uiTorrentInfo.set(null);
            return;
        }

        List<UiFileInfo> selectedUiFilesInfos = torrent.getFilesWithInfo().stream()
                .map(UiFileInfo::fromDomain)
                .toList();
        Platform.runLater(() -> uiFileInfos.set(FXCollections.observableList(selectedUiFilesInfos)));

        UiTorrentInfo selectedUiTorrentInfo = UiTorrentInfo.fromDomain(torrent);
        Platform.runLater(() -> uiTorrentInfo.set(selectedUiTorrentInfo));
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

    public void addTorrent(UiTorrentContents uiTorrentContents) {
        Torrent torrent = uiTorrentContents.getTorrent();
        client.addTorrent(torrent);
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
}
