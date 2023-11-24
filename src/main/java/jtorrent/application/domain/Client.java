package jtorrent.application.domain;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jtorrent.common.domain.util.BackgroundTask;
import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.common.domain.util.rx.RxObservableList;
import jtorrent.dht.domain.handler.DhtClient;
import jtorrent.dht.domain.handler.DhtManager;
import jtorrent.incoming.domain.InboundConnectionListener;
import jtorrent.lsd.domain.handler.LocalServiceDiscoveryManager;
import jtorrent.lsd.domain.model.Announce;
import jtorrent.peer.domain.communication.PeerSocket;
import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.torrent.domain.handler.TorrentHandler;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.torrent.domain.repository.PieceRepository;
import jtorrent.torrent.domain.repository.TorrentRepository;

public class Client implements LocalServiceDiscoveryManager.Listener, TorrentHandler.Listener,
        DhtManager.PeerDiscoveryListener {

    private static final Logger LOGGER = System.getLogger(Client.class.getName());

    private final InboundConnectionListener inboundConnectionListener;
    private final LocalServiceDiscoveryManager localServiceDiscoveryManager;
    private final DhtClient dhtManager;
    private final Map<Sha1Hash, TorrentHandler> infoHashToTorrentHandler = new HashMap<>();
    private final TorrentRepository torrentRepository;
    private final PieceRepository pieceRepository;
    private final HandleInboundConnectionsTask handleInboundConnectionsTask = new HandleInboundConnectionsTask();

    public Client(TorrentRepository torrentRepository, PieceRepository pieceRepository,
            InboundConnectionListener inboundConnectionListener,
            LocalServiceDiscoveryManager localServiceDiscoveryManager, DhtClient dhtClient) {
        this.torrentRepository = torrentRepository;
        this.pieceRepository = pieceRepository;

        this.inboundConnectionListener = inboundConnectionListener;
        this.inboundConnectionListener.start();
        handleInboundConnectionsTask.start();

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
        inboundConnectionListener.stop();
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

    private class HandleInboundConnectionsTask extends BackgroundTask {

        @Override
        protected void execute() throws InterruptedException {
            InboundConnectionListener.InboundConnection inboundConnection =
                    inboundConnectionListener.waitForIncomingConnection();

            Sha1Hash infoHash = inboundConnection.getInfoHash();
            if (!infoHashToTorrentHandler.containsKey(infoHash)) {
                LOGGER.log(Level.ERROR, "No torrent found for infohash " + infoHash);
                try {
                    inboundConnection.reject();
                } catch (IOException e) {
                    LOGGER.log(Level.ERROR, "Error rejecting incoming connection", e);
                }
                return;
            }

            PeerSocket peerSocket = inboundConnection.accept();
            TorrentHandler torrentHandler = infoHashToTorrentHandler.get(infoHash);
            torrentHandler.handleInboundPeerConnection(peerSocket);
        }
    }
}
