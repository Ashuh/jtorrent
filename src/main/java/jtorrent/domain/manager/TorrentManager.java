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
import jtorrent.domain.repository.TorrentRepository;
import jtorrent.domain.util.RxObservableList;
import jtorrent.domain.util.Sha1Hash;

public class TorrentManager implements IncomingConnectionManager.Listener, LocalServiceDiscoveryManager.Listener {

    private static final Logger LOGGER = System.getLogger(TorrentManager.class.getName());

    private final IncomingConnectionManager incomingConnectionManager;
    private final LocalServiceDiscoveryManager localServiceDiscoveryManager;
    private final Map<Sha1Hash, TorrentHandler> infoHashToTorrentHandler = new HashMap<>();
    private final TorrentRepository torrentRepository;

    public TorrentManager(TorrentRepository torrentRepository, IncomingConnectionManager incomingConnectionManager,
            LocalServiceDiscoveryManager localServiceDiscoveryManager) {
        this.torrentRepository = torrentRepository;

        this.incomingConnectionManager = incomingConnectionManager;
        this.incomingConnectionManager.addListener(this);
        this.incomingConnectionManager.start();

        this.localServiceDiscoveryManager = localServiceDiscoveryManager;
        this.localServiceDiscoveryManager.addListener(this);
        this.localServiceDiscoveryManager.start();

        torrentRepository.getTorrents().subscribe(event -> {
            switch (event.getType()) {
            case ADD:
                startTorrent(event.getItem());
                break;
            case REMOVE:
                stopTorrent(event.getItem());
                break;
            case CLEAR:
                infoHashToTorrentHandler.values().forEach(TorrentHandler::stop);
                infoHashToTorrentHandler.clear();
                break;
            default:
                throw new AssertionError("Unknown event type: " + event.getType());
            }
        });
    }

    public void shutdown() {
        incomingConnectionManager.stop();
        localServiceDiscoveryManager.stop();
        infoHashToTorrentHandler.values().forEach(TorrentHandler::stop);
    }

    public void addTorrent(Torrent torrent) {
        torrentRepository.addTorrent(torrent);
    }

    public void startTorrent(Torrent torrent) {
        LOGGER.log(Level.INFO, "Starting torrent " + torrent.getName());
        TorrentHandler torrentHandler = new TorrentHandler(torrent);
        infoHashToTorrentHandler.put(torrent.getInfoHash(), torrentHandler);
        torrentHandler.start();
        localServiceDiscoveryManager.addInfoHash(torrent.getInfoHash());
    }

    public void stopTorrent(Torrent torrent) {
        LOGGER.log(Level.INFO, "Stopping torrent " + torrent.getName());
        TorrentHandler torrentHandler = infoHashToTorrentHandler.remove(torrent.getInfoHash());
        torrentHandler.stop();
        // TODO: remove from local service discovery
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

    public RxObservableList<Torrent> getTorrents() {
        return torrentRepository.getTorrents();
    }

    @Override
    public void onAnnounceReceived(Announce announce, InetAddress sourceAddress) {
        announce.getInfoHashes().stream()
                .filter(infoHashToTorrentHandler::containsKey)
                .map(infoHashToTorrentHandler::get)
                .forEach(torrentHandler -> {
                    OutgoingPeer peer = new OutgoingPeer(new PeerContactInfo(sourceAddress, announce.getPort()));
                    torrentHandler.addPeer(peer);
                });
    }
}
