package jtorrent.torrent.domain.repository;

import java.io.File;
import java.io.IOException;

import jtorrent.common.domain.util.rx.RxObservableList;
import jtorrent.torrent.domain.model.Torrent;

public interface TorrentRepository {

    void addTorrent(Torrent torrent);

    Torrent loadTorrent(File file) throws IOException;

    void removeTorrent(Torrent torrent);

    RxObservableList<Torrent> getTorrents();
}
