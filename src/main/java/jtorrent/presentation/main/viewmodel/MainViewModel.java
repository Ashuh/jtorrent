package jtorrent.presentation.main.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import jtorrent.domain.Client;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.presentation.addnewtorrent.view.AddNewTorrentDialog;

public class MainViewModel {

    private final Client client;
    private final TorrentControlsViewModel torrentControlsViewModel;
    private final TorrentsTableViewModel torrentsTableViewModel;
    private final TorrentInfoViewModel torrentInfoViewModel;
    private final FileInfoViewModel fileInfoViewModel;
    private final PeersTableViewModel peersTableViewModel;
    private final ChartViewModel chartViewModel;

    public MainViewModel(Client client) {
        this.client = requireNonNull(client);
        torrentControlsViewModel = new TorrentControlsViewModel(client);
        torrentsTableViewModel = new TorrentsTableViewModel(client, this::onTorrentSelected);
        torrentInfoViewModel = new TorrentInfoViewModel(client);
        fileInfoViewModel = new FileInfoViewModel(client);
        peersTableViewModel = new PeersTableViewModel(client);
        chartViewModel = new ChartViewModel(client);
    }

    private void onTorrentSelected(Torrent torrent) {
        torrentControlsViewModel.setSelectedTorrent(torrent);
        torrentInfoViewModel.setSelectedTorrent(torrent);
        fileInfoViewModel.setSelectedTorrent(torrent);
        peersTableViewModel.setSelectedTorrent(torrent);
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

    public TorrentMetadata loadTorrent(File file) throws IOException {
        return client.loadTorrentMetadata(file);
    }

    public TorrentMetadata loadTorrent(String urlString) throws IOException {
        URL url = new URL(urlString);
        return client.loadTorrentMetadata(url);
    }

    public void addTorrent(TorrentMetadata torrentMetadata, AddNewTorrentDialog.Result result) {
        client.addTorrent(torrentMetadata, result.name(), Path.of(result.saveDirectory()));
    }
}
