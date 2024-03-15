package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    private final FileInfoViewModel fileInfoViewModel;
    private final PeersTableViewModel peersTableViewModel;
    private final ChartViewModel chartViewModel;

    public ViewModel(Client client) {
        this.client = requireNonNull(client);
        torrentControlsViewModel = new TorrentControlsViewModel(client);
        torrentsTableViewModel = new TorrentsTableViewModel(client, this::onTorrentSelected);
        torrentInfoViewModel = new TorrentInfoViewModel(client);
        fileInfoViewModel = new FileInfoViewModel(client);
        peersTableViewModel = new PeersTableViewModel(client);
        chartViewModel = new ChartViewModel(client);
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

    public FileInfoViewModel getFileInfoViewModel() {
        return fileInfoViewModel;
    }

    public PeersTableViewModel getPeersTableViewModel() {
        return peersTableViewModel;
    }

    public ChartViewModel getChartViewModel() {
        return chartViewModel;
    }

    private void onTorrentSelected(Torrent torrent) {
        torrentControlsViewModel.setSelectedTorrent(torrent);
        torrentInfoViewModel.setSelectedTorrent(torrent);
        fileInfoViewModel.setSelectedTorrent(torrent);
        peersTableViewModel.setSelectedTorrent(torrent);
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
}
