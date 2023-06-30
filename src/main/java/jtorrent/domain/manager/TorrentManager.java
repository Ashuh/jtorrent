package jtorrent.domain.manager;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import jtorrent.domain.handler.torrent.TorrentHandler;
import jtorrent.domain.model.localservicediscovery.Announce;
import jtorrent.domain.model.peer.OutgoingPeer;
import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.util.Sha1Hash;

public class TorrentManager implements IncomingConnectionManager.Listener, LocalServiceDiscoveryManager.Listener {

    private static final Logger LOGGER = System.getLogger(TorrentManager.class.getName());

    private final IncomingConnectionManager incomingConnectionManager;
    private final LocalServiceDiscoveryManager localServiceDiscoveryManager;
    private final Map<Sha1Hash, TorrentHandler> infoHashToTorrentHandler = new HashMap<>();


    public TorrentManager(IncomingConnectionManager incomingConnectionManager,
            LocalServiceDiscoveryManager localServiceDiscoveryManager) {
        this.incomingConnectionManager = incomingConnectionManager;
        this.incomingConnectionManager.addListener(this);
        this.localServiceDiscoveryManager = localServiceDiscoveryManager;
        this.localServiceDiscoveryManager.addListener(this);
    }

    public void addTorrent(Torrent torrent) {
        TorrentHandler torrentHandler = new TorrentHandler(torrent);
        infoHashToTorrentHandler.put(torrent.getInfoHash(), torrentHandler);
        torrentHandler.start();
        localServiceDiscoveryManager.addInfoHash(torrent.getInfoHash());
    }

    @Override
    public void onIncomingPeerConnection(Peer peer, Sha1Hash infoHash) {
        if (!infoHashToTorrentHandler.containsKey(infoHash)) {
            LOGGER.log(Level.INFO, "No torrent found for infohash " + infoHash);
            return;
        }

        TorrentHandler torrentHandler = infoHashToTorrentHandler.get(infoHash);
        torrentHandler.addPeer(peer);
    }

    @Override
    public void onAnnounceReceived(Announce announce, InetAddress sourceAddress) {
        announce.getInfoHashes().stream()
                .filter(infoHashToTorrentHandler::containsKey)
                .map(infoHashToTorrentHandler::get)
                .forEach(torrentHandler -> {
                    OutgoingPeer peer = new OutgoingPeer(sourceAddress, announce.getPort());
                    torrentHandler.addPeer(peer);
                });
    }
}
