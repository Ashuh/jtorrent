package jtorrent.presentation.main.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import jtorrent.data.torrent.model.BencodedTorrent;
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

    public BencodedTorrent loadTorrent(File file) throws IOException {
        return client.loadTorrent(file);
    }

    public BencodedTorrent loadTorrent(String urlString) throws IOException {
        URL url = new URL(urlString);
        return client.loadTorrent(url);
    }

    public void addTorrent(BencodedTorrent bencodedTorrent, AddNewTorrentDialog.Result result) {
        TorrentMetadata torrentMetaData = bencodedTorrent.toDomain();
        client.addTorrent(torrentMetaData, result.name(), Path.of(result.saveDirectory()));
    }
}
