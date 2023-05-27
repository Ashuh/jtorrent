package jtorrent.domain.repository;

import java.io.IOException;
import java.nio.file.Path;

import jtorrent.domain.model.torrent.Torrent;

public interface TorrentRepository {

    Torrent getTorrent(Path path) throws IOException;
}
