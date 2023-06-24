package jtorrent.domain.manager;

import java.lang.System.Logger;
import java.util.HashMap;
import java.util.Map;

import jtorrent.domain.handler.torrent.TorrentHandler;
import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.util.Sha1Hash;

public class TorrentManager implements IncomingConnectionManager.Listener {

    private static final Logger LOGGER = System.getLogger(TorrentManager.class.getName());

    private final IncomingConnectionManager incomingConnectionManager;
    private final Map<Sha1Hash, TorrentHandler> infoHashToTorrentHandler = new HashMap<>();


    public TorrentManager(IncomingConnectionManager incomingConnectionManager) {
        this.incomingConnectionManager = incomingConnectionManager;
        this.incomingConnectionManager.addListener(this);
    }

    public void addTorrent(Torrent torrent) {
        TorrentHandler torrentHandler = new TorrentHandler(torrent);
        infoHashToTorrentHandler.put(torrent.getInfoHash(), torrentHandler);
        torrentHandler.start();
    }

    @Override
    public void onIncomingPeerConnection(Peer peer, Sha1Hash infoHash) {
        if (!infoHashToTorrentHandler.containsKey(infoHash)) {
            LOGGER.log(Logger.Level.INFO, "No torrent found for infohash " + infoHash);
            return;
        }

        TorrentHandler torrentHandler = infoHashToTorrentHandler.get(infoHash);
        torrentHandler.addPeer(peer);
    }
}
