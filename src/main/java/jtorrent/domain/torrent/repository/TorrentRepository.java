package jtorrent.domain.torrent.repository;

import java.io.File;
import java.io.IOException;

import jtorrent.domain.common.util.rx.RxObservableList;
import jtorrent.domain.torrent.model.Torrent;

public interface TorrentRepository {

    void addTorrent(Torrent torrent);

    Torrent loadTorrent(File file) throws IOException;

    void removeTorrent(Torrent torrent);

    RxObservableList<Torrent> getTorrents();
}