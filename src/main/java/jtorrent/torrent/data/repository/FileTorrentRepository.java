package jtorrent.torrent.data.repository;

import java.io.File;
import java.io.FileInputStream;
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
            loadTorrent(Path.of("ubuntu-23.04-desktop-amd64.iso.torrent"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadTorrent(Path path) throws IOException {
        File file = path.toFile();
        loadTorrent(file);
    }

    @Override
    public void loadTorrent(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        BencodedTorrent bencodedTorrent = BencodedTorrent.decode(inputStream);
        addTorrent(bencodedTorrent.toDomain());
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
