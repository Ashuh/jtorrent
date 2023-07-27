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
import jtorrent.domain.util.MutableRxObservableList;
import jtorrent.domain.util.RxObservableList;

public class FileTorrentRepository implements TorrentRepository {

    private final MutableRxObservableList<Torrent> torrents = new MutableRxObservableList<>();

    public FileTorrentRepository() {
        // TODO: temporary
        try {
            Torrent torrent = loadTorrent(Path.of("ubuntu-23.04-desktop-amd64.iso.torrent"));
            torrents.add(torrent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Torrent loadTorrent(Path path) throws IOException {
        File file = path.toFile();

        if (!file.exists()) {
            throw new FileNotFoundException("Unable to find file " + path);
        }

        InputStream inputStream = new FileInputStream(file);
        BencodedTorrent bencodedTorrent = BencodedTorrent.decode(inputStream);
        return bencodedTorrent.toDomain();
    }

    @Override
    public void addTorrent(Torrent torrent) {
        torrents.add(torrent);
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        torrents.remove(torrent);
    }

    @Override
    public RxObservableList<Torrent> getTorrents() {
        return torrents;
    }
}
