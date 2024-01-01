package jtorrent.torrent.data.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.common.domain.util.rx.MutableRxObservableList;
import jtorrent.common.domain.util.rx.RxObservableList;
import jtorrent.torrent.data.model.BencodedTorrent;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.torrent.domain.repository.TorrentRepository;

public class FileTorrentRepository implements TorrentRepository {

    private final MutableRxObservableList<Torrent> torrents = new MutableRxObservableList<>(new ArrayList<>());
    private final Map<Sha1Hash, Torrent> infoHashToTorrent = new HashMap<>();

    public FileTorrentRepository() {
        // TODO: temporary
        try {
            addTorrent(loadTorrent(Path.of("ubuntu-23.04-desktop-amd64.iso.torrent")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Torrent loadTorrent(Path path) throws IOException {
        File file = path.toFile();
        return loadTorrent(file);
    }

    @Override
    public Torrent loadTorrent(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        BencodedTorrent bencodedTorrent = BencodedTorrent.decode(inputStream);
        return bencodedTorrent.toDomain();
    }

    @Override
    public void addTorrent(Torrent torrent) {
        if (isExistingTorrent(torrent)) {
            // TODO: maybe throw exception if torrent already exists?
            return;
        }
        infoHashToTorrent.put(torrent.getInfoHash(), torrent);
        torrents.add(torrent);
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        infoHashToTorrent.remove(torrent.getInfoHash());
        torrents.remove(torrent);
    }

    @Override
    public RxObservableList<Torrent> getTorrents() {
        return torrents;
    }

    private boolean isExistingTorrent(Torrent torrent) {
        return infoHashToTorrent.containsKey(torrent.getInfoHash());
    }
}
