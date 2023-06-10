package jtorrent.domain.manager;

import java.util.HashMap;
import java.util.Map;

import jtorrent.domain.handler.torrent.TorrentHandler;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.util.Sha1Hash;

public class TorrentManager {

    private final Map<Sha1Hash, TorrentHandler> infoHashToTorrentHandler = new HashMap<>();

    public void addTorrent(Torrent torrent) {
        TorrentHandler torrentHandler = new TorrentHandler(torrent);
        infoHashToTorrentHandler.put(torrent.getInfoHash(), torrentHandler);
        torrentHandler.start();
    }
}
