package jtorrent.domain.torrent.repository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import jtorrent.domain.torrent.model.TorrentMetadata;

public interface TorrentMetadataRepository {

    TorrentMetadata getTorrentMetadata(File file) throws IOException;

    TorrentMetadata getTorrentMetadata(URL url) throws IOException;

    void saveTorrentMetadata(TorrentMetadata torrentMetadata, Path savePath) throws IOException;

    TorrentMetadata createNewTorrent(Path source, List<List<String>> trackerUrls, String comment, String createdBy,
            int pieceSize) throws IOException;
}
