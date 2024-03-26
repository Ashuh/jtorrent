package jtorrent.domain.torrent.repository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.rx.RxObservableList;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.model.TorrentMetadata;

public interface TorrentRepository {

    void addTorrent(Torrent torrent);

    TorrentMetadata loadTorrent(File file) throws IOException;

    TorrentMetadata loadTorrent(URL url) throws IOException;

    void saveTorrent(TorrentMetadata torrentMetadata, Path savePath) throws IOException;

    void removeTorrent(Torrent torrent);

    RxObservableList<Torrent> getTorrents();

    Torrent getTorrent(Sha1Hash infoHash);

    TorrentMetadata createNewTorrent(Path source, List<List<String>> trackerUrls, String comment, String createdBy,
            int pieceSize) throws IOException;
}
