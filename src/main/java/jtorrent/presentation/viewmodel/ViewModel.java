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

public class ViewModel {

    private final Client client;
    private final ObjectProperty<UiChartData> chartData = new SimpleObjectProperty<>();
    private final ObjectProperty<ObservableList<UiFileInfo>> uiFileInfos = new SimpleObjectProperty<>();
    private final TorrentControlsViewModel torrentControlsViewModel;
    private final TorrentsTableViewModel torrentsTableViewModel;
    private final TorrentInfoViewModel torrentInfoViewModel;
    private final PeersTableViewModel peersTableViewModel;

    public ViewModel(Client client) {
        this.client = requireNonNull(client);
        chartData.set(UiChartData.build(client));
        torrentControlsViewModel = new TorrentControlsViewModel(client);
        torrentsTableViewModel = new TorrentsTableViewModel(client, this::onTorrentSelected);
        torrentInfoViewModel = new TorrentInfoViewModel(client);
        peersTableViewModel = new PeersTableViewModel(client);
    }

    public TorrentControlsViewModel getTorrentControlsViewModel() {
        return torrentControlsViewModel;
    }

    public TorrentsTableViewModel getTorrentsTableViewModel() {
        return torrentsTableViewModel;
    }

    public TorrentInfoViewModel getTorrentInfoViewModel() {
        return torrentInfoViewModel;
    }

    public PeersTableViewModel getPeersTableViewModel() {
        return peersTableViewModel;
    }

    private void onTorrentSelected(Torrent torrent) {
        torrentControlsViewModel.setSelectedTorrent(torrent);
        torrentInfoViewModel.setSelectedTorrent(torrent);
        peersTableViewModel.setSelectedTorrent(torrent);

        if (uiFileInfos.get() != null) {
            uiFileInfos.get().forEach(UiFileInfo::dispose);
        }

        if (torrent == null) {
            uiFileInfos.set(null);
            return;
        }

        List<UiFileInfo> selectedUiFilesInfos = torrent.getFilesWithInfo().stream()
                .map(UiFileInfo::fromDomain)
                .toList();
        Platform.runLater(() -> uiFileInfos.set(FXCollections.observableList(selectedUiFilesInfos)));
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

    public ReadOnlyObjectProperty<UiChartData> chartDataProperty() {
        return chartData;
    }
}
