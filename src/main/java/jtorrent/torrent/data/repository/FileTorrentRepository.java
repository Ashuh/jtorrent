package jtorrent.torrent.data.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;

import jtorrent.common.domain.util.rx.MutableRxObservableList;
import jtorrent.common.domain.util.rx.RxObservableList;
import jtorrent.torrent.data.model.BencodedTorrent;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.torrent.domain.repository.TorrentRepository;

public class FileTorrentRepository implements TorrentRepository {

    private final MutableRxObservableList<Torrent> torrents = new MutableRxObservableList<>(new ArrayList<>());

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
