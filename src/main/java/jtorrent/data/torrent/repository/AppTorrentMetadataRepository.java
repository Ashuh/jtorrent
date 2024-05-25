package jtorrent.data.torrent.repository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import jtorrent.data.torrent.source.file.filemanager.BencodedTorrentFileManager;
import jtorrent.data.torrent.source.file.model.BencodedTorrent;
import jtorrent.data.torrent.source.file.model.BencodedTorrentFactory;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.domain.torrent.repository.TorrentMetadataRepository;

public class AppTorrentMetadataRepository implements TorrentMetadataRepository {

    private final BencodedTorrentFileManager torrentFileManager = new BencodedTorrentFileManager();

    @Override
    public TorrentMetadata getTorrentMetadata(File file) throws IOException {
        return torrentFileManager.read(file).toDomain();
    }

    @Override
    public TorrentMetadata getTorrentMetadata(URL url) throws IOException {
        return torrentFileManager.read(url).toDomain();
    }

    @Override
    public void saveTorrentMetadata(TorrentMetadata torrentMetadata, Path savePath) throws IOException {
        BencodedTorrent bencodedTorrent = BencodedTorrent.fromDomain(torrentMetadata);
        torrentFileManager.write(savePath, bencodedTorrent);
    }

    /**
     * Create a new {@link TorrentMetadata} instance with the current time as the creation date.
     *
     * @param trackerUrls list of tiers, each containing a list of tracker URLs
     * @param comment     comment about the torrent
     * @param createdBy   name and version of the program used to create the .torrent
     * @param pieceSize   size of each piece in bytes
     * @return a new {@link TorrentMetadata} instance
     */
    @Override
    public TorrentMetadata createNewTorrent(Path source, List<List<String>> trackerUrls, String comment,
            String createdBy, int pieceSize) throws IOException {
        return BencodedTorrentFactory.create(source, trackerUrls, comment, createdBy, pieceSize).toDomain();
    }
}
