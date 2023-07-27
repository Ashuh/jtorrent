package jtorrent.domain.repository;

import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.util.RxObservableList;

public interface TorrentRepository {

    void addTorrent(Torrent torrent);

    void removeTorrent(Torrent torrent);

    RxObservableList<Torrent> getTorrents();
}
