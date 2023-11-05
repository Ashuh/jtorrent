package jtorrent.domain.manager;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jtorrent.domain.handler.torrent.TorrentHandler;
import jtorrent.domain.manager.dht.DhtClient;
import jtorrent.domain.manager.dht.DhtManager;
import jtorrent.domain.model.localservicediscovery.Announce;
import jtorrent.domain.model.peer.PeerContactInfo;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.repository.PieceRepository;
import jtorrent.domain.repository.TorrentRepository;
import jtorrent.domain.socket.PeerSocket;
import jtorrent.domain.util.RxObservableList;
import jtorrent.domain.util.Sha1Hash;

public class Client implements IncomingConnectionManager.Listener, LocalServiceDiscoveryManager.Listener,
        TorrentHandler.Listener, DhtManager.PeerDiscoveryListener {

    private static final Logger LOGGER = System.getLogger(Client.class.getName());

    private final IncomingConnectionManager incomingConnectionManager;
    private final LocalServiceDiscoveryManager localServiceDiscoveryManager;
    private final DhtClient dhtManager;
    private final Map<Sha1Hash, TorrentHandler> infoHashToTorrentHandler = new HashMap<>();
    private final TorrentRepository torrentRepository;
    private final PieceRepository pieceRepository;

    public Client(TorrentRepository torrentRepository, PieceRepository pieceRepository,
            IncomingConnectionManager incomingConnectionManager,
            LocalServiceDiscoveryManager localServiceDiscoveryManager, DhtClient dhtClient) {
        this.torrentRepository = torrentRepository;
        this.pieceRepository = pieceRepository;

        this.incomingConnectionManager = incomingConnectionManager;
        this.incomingConnectionManager.addListener(this);
        this.incomingConnectionManager.start();

        this.localServiceDiscoveryManager = localServiceDiscoveryManager;
        this.localServiceDiscoveryManager.addListener(this);
        this.localServiceDiscoveryManager.start();

        this.dhtManager = dhtClient;
        this.dhtManager.addPeerDiscoveryListener(this);
        this.dhtManager.start();

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
        dhtManager.stop();
        infoHashToTorrentHandler.values().forEach(TorrentHandler::stop);
    }

    public void addTorrent(Torrent torrent) {
        torrentRepository.addTorrent(torrent);
    }

    public void startTorrent(Torrent torrent) {
        LOGGER.log(Level.INFO, "Starting torrent " + torrent.getName());
        TorrentHandler torrentHandler = new TorrentHandler(torrent, pieceRepository);
        infoHashToTorrentHandler.put(torrent.getInfoHash(), torrentHandler);
        torrentHandler.addListener(this);
        torrentHandler.start();
        localServiceDiscoveryManager.addInfoHash(torrent.getInfoHash());
        dhtManager.registerInfoHash(torrent.getInfoHash());
    }

    public void stopTorrent(Torrent torrent) {
        LOGGER.log(Level.INFO, "Stopping torrent " + torrent.getName());
        TorrentHandler torrentHandler = infoHashToTorrentHandler.remove(torrent.getInfoHash());
        torrentHandler.stop();
        dhtManager.deregisterInfoHash(torrent.getInfoHash());
        // TODO: remove from local service discovery
    }

    @Override
    public void onIncomingPeerConnection(PeerSocket peerSocket, Sha1Hash infoHash) {
        if (!infoHashToTorrentHandler.containsKey(infoHash)) {
            LOGGER.log(Level.ERROR, "No torrent found for infohash " + infoHash);
            return;
        }

        TorrentHandler torrentHandler = infoHashToTorrentHandler.get(infoHash);
        torrentHandler.handleIncomingPeerConnection(peerSocket);
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
                    PeerContactInfo peerContactInfo = new PeerContactInfo(sourceAddress, announce.getPort());
                    torrentHandler.handleDiscoveredPeerContact(peerContactInfo);
                });
    }

    @Override
    public void onDhtNodeDiscovered(InetSocketAddress address) {
        LOGGER.log(Level.DEBUG, "DHT node discovered: " + address);
        dhtManager.addBootstrapNodeAddress(address);
    }

    @Override
    public void onPeersDiscovered(Sha1Hash infoHash, Collection<PeerContactInfo> peers) {
        LOGGER.log(Level.INFO, "Discovered " + peers.size() + " peers for info hash " + infoHash);
        if (!infoHashToTorrentHandler.containsKey(infoHash)) {
            LOGGER.log(Level.ERROR, "No torrent found for infohash " + infoHash);
            return;
        }

        TorrentHandler torrentHandler = infoHashToTorrentHandler.get(infoHash);
        peers.forEach(torrentHandler::handleDiscoveredPeerContact);
    }
}
