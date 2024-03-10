package jtorrent.data.torrent.repository;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jtorrent.data.torrent.model.BencodedTorrent;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.rx.MutableRxObservableList;
import jtorrent.domain.common.util.rx.RxObservableList;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.repository.TorrentRepository;

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
    public Torrent loadTorrent(URL url) throws IOException {
        // For some reason decoding directly from the URL stream doesn't work, so we have to read it into a byte array
        // first.
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream();
        ) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }

            try (InputStream inputStream = new ByteArrayInputStream(out.toByteArray())) {
                BencodedTorrent bencodedTorrent = BencodedTorrent.decode(inputStream);
                return bencodedTorrent.toDomain();
            }
        }
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
    public void saveTorrent(BencodedTorrent bencodedTorrent, Path savePath) throws IOException {
        try (var outputStream = Files.newOutputStream(savePath)) {
            outputStream.write(bencodedTorrent.bencode());
        }
    }

    @Override
    public RxObservableList<Torrent> getTorrents() {
        return torrents;
    }

    private boolean isExistingTorrent(Torrent torrent) {
        return infoHashToTorrent.containsKey(torrent.getInfoHash());
    }
}
