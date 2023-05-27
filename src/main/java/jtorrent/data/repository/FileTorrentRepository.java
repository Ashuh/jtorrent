package jtorrent.data.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import jtorrent.data.model.torrent.BencodedTorrent;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.repository.TorrentRepository;

public class FileTorrentRepository implements TorrentRepository {

    @Override
    public Torrent getTorrent(Path path) throws IOException {
        File file = path.toFile();

        if (!file.exists()) {
            throw new FileNotFoundException("Unable to find file " + path);
        }

        InputStream inputStream = new FileInputStream(file);
        BencodedTorrent bencodedTorrent = BencodedTorrent.decode(inputStream);
        return bencodedTorrent.toDomain();
    }
}
