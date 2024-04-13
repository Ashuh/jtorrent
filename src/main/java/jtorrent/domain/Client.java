package jtorrent.domain;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.BackgroundTask;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.common.util.logging.MdcUtil;
import jtorrent.domain.common.util.rx.RxObservableList;
import jtorrent.domain.dht.DhtClient;
import jtorrent.domain.dht.handler.DhtManager;
import jtorrent.domain.inbound.InboundConnectionListener;
import jtorrent.domain.lsd.LocalServiceDiscoveryManager;
import jtorrent.domain.lsd.model.Announce;
import jtorrent.domain.peer.communication.PeerSocket;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.torrent.handler.TorrentHandler;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.domain.torrent.repository.PieceRepository;
import jtorrent.domain.torrent.repository.TorrentRepository;

public class Client implements LocalServiceDiscoveryManager.Listener, TorrentHandler.Listener,
        DhtManager.PeerDiscoveryListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

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
        handleInboundConnectionsTask.stop();
        localServiceDiscoveryManager.stop();
        dhtManager.stop();
        infoHashToTorrentHandler.values().forEach(TorrentHandler::stop);
    }

    public void addTorrent(TorrentMetadata torrentMetaData, String name, Path saveDirectory) {
        Torrent torrent = new Torrent(torrentMetaData, name, saveDirectory);
        torrentRepository.addTorrent(torrent);
    }

    public void removeTorrent(Torrent torrent) {
        torrentRepository.removeTorrent(torrent);
    }

    public TorrentMetadata loadTorrent(File file) throws IOException {
        return torrentRepository.loadTorrent(file);
    }

    public TorrentMetadata loadTorrent(URL url) throws IOException {
        return torrentRepository.loadTorrent(url);
    }

    public void startTorrent(Torrent torrent) {
        MdcUtil.putTorrent(torrent);
        TorrentHandler torrentHandler = new TorrentHandler(torrent, pieceRepository);
        infoHashToTorrentHandler.put(torrent.getInfoHash(), torrentHandler);
        torrentHandler.addListener(this);
        torrentHandler.start();
        localServiceDiscoveryManager.addInfoHash(torrent.getInfoHash());
        dhtManager.registerInfoHash(torrent.getInfoHash());
        LOGGER.info(Markers.TORRENT, "Torrent started");
        MdcUtil.removeTorrent();
    }

    public void stopTorrent(Torrent torrent) {
        MdcUtil.putTorrent(torrent);
        TorrentHandler torrentHandler = infoHashToTorrentHandler.remove(torrent.getInfoHash());
        if (torrentHandler != null) {
            torrentHandler.stop();
        }
        dhtManager.deregisterInfoHash(torrent.getInfoHash());
        // TODO: remove from local service discovery
        LOGGER.info(Markers.TORRENT, "Torrent stopped");
        MdcUtil.removeTorrent();
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
                    MdcUtil.putTorrent(torrentHandler);
                    PeerContactInfo peerContactInfo = new PeerContactInfo(sourceAddress, announce.getPort());
                    torrentHandler.handleDiscoveredPeerContact(peerContactInfo);
                    MdcUtil.removeTorrent();
                });
    }

    @Override
    public void onDhtNodeDiscovered(InetSocketAddress address) {
        LOGGER.debug(Markers.DHT, "DHT node discovered: {}", address);
        dhtManager.addBootstrapNodeAddress(address);
    }

    @Override
    public void onPeersDiscovered(Sha1Hash infoHash, Collection<PeerContactInfo> peers) {
        if (!infoHashToTorrentHandler.containsKey(infoHash)) {
            LOGGER.warn(Markers.DHT, "No torrent found with info hash {}", infoHash);
            return;
        }

        LOGGER.info(Markers.DHT, "Discovered {} peers for info hash {}", peers.size(), infoHash);
        TorrentHandler torrentHandler = infoHashToTorrentHandler.get(infoHash);
        MdcUtil.putTorrent(torrentHandler);
        peers.forEach(torrentHandler::handleDiscoveredPeerContact);
        MdcUtil.removeTorrent();
    }

    public void addPeer(Torrent torrent, PeerContactInfo peerContactInfo) {
        // TODO: only works if the torrent is active. If it is not active, should we store it somewhere else first?
        TorrentHandler torrentHandler = infoHashToTorrentHandler.get(torrent.getInfoHash());
        if (torrentHandler != null) {
            MdcUtil.putTorrent(torrentHandler);
            torrentHandler.handleDiscoveredPeerContact(peerContactInfo);
            MdcUtil.removeTorrent();
        }
    }

    public double getDownloadRate() {
        return torrentRepository.getTorrents().getCollection().stream()
                .mapToDouble(Torrent::getDownloadRate)
                .sum();
    }

    public double getUploadRate() {
        return torrentRepository.getTorrents().getCollection().stream()
                .mapToDouble(Torrent::getUploadRate)
                .sum();
    }

    public void createNewTorrent(Path savePath, Path source, List<List<String>> trackerUrls, String comment,
            int pieceSize) throws IOException {
        TorrentMetadata torrentMetadata = torrentRepository.createNewTorrent(source, trackerUrls, comment,
                "JTorrent", pieceSize);
        torrentRepository.saveTorrent(torrentMetadata, savePath);
    }

    public Torrent getTorrent(Sha1Hash infoHash) {
        return torrentRepository.getTorrent(infoHash);
    }

    private class HandleInboundConnectionsTask extends BackgroundTask {

        @Override
        protected void execute() throws InterruptedException {
            InboundConnectionListener.InboundConnection inboundConnection =
                    inboundConnectionListener.waitForIncomingConnection();

            Sha1Hash infoHash = inboundConnection.getInfoHash();
            if (!infoHashToTorrentHandler.containsKey(infoHash)) {
                LOGGER.warn(Markers.INBOUND, "No torrent found for info hash {}", infoHash);
                try {
                    inboundConnection.reject();
                } catch (IOException e) {
                    LOGGER.error(Markers.INBOUND, "Failed to reject incoming connection", e);
                }
                return;
            }

            PeerSocket peerSocket = inboundConnection.accept();
            TorrentHandler torrentHandler = infoHashToTorrentHandler.get(infoHash);
            MdcUtil.putTorrent(torrentHandler);
            torrentHandler.handleInboundPeerConnection(peerSocket);
            MdcUtil.removeTorrent();
        }
    }
}
