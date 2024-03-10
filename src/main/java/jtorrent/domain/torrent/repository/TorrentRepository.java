package jtorrent.domain.torrent.repository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import jtorrent.data.torrent.model.BencodedTorrent;
import jtorrent.domain.common.util.rx.RxObservableList;
import jtorrent.domain.torrent.model.Torrent;

public interface TorrentRepository {

    void addTorrent(Torrent torrent);

    Torrent loadTorrent(File file) throws IOException;

    Torrent loadTorrent(URL url) throws IOException;

    void saveTorrent(BencodedTorrent bencodedTorrent, Path savePath) throws IOException;

    void removeTorrent(Torrent torrent);

    RxObservableList<Torrent> getTorrents();
}
