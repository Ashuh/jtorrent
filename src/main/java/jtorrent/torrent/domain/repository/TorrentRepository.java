package jtorrent.torrent.domain.repository;

import jtorrent.common.domain.util.rx.RxObservableList;
import jtorrent.torrent.domain.model.Torrent;

public interface TorrentRepository {

    void addTorrent(Torrent torrent);

    void removeTorrent(Torrent torrent);

    RxObservableList<Torrent> getTorrents();
}
