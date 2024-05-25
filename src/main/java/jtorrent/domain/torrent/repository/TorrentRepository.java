package jtorrent.domain.torrent.repository;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.rx.RxObservableList;
import jtorrent.domain.torrent.model.Torrent;

public interface TorrentRepository {

    RxObservableList<Torrent> getTorrents();

    Torrent getTorrent(Sha1Hash infoHash);

    void addTorrent(Torrent torrent);

    void persistTorrents();

    void removeTorrent(Torrent torrent);
}
