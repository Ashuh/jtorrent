package jtorrent.domain.dht;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.dht.communication.DhtSocket;
import jtorrent.domain.dht.handler.DhtManager;
import jtorrent.domain.dht.handler.DhtQueryHandler;
import jtorrent.domain.dht.handler.PeerContactInfoStore;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.handler.routingtable.RoutingTable;

public class DhtClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DhtClient.class);

    private final DhtSocket dhtSocket;
    private final DhtManager dhtManager;

    public DhtClient(int port) throws SocketException {
        RoutingTable routingTable = new RoutingTable();
        PeerContactInfoStore peerContactInfoStore = new PeerContactInfoStore();
        DhtSocket.QueryHandler queryHandler = new DhtQueryHandler(routingTable, peerContactInfoStore);
        this.dhtSocket = new DhtSocket(new DatagramSocket(port), queryHandler);
        this.dhtManager = new DhtManager(routingTable);
        Node.setDhtSocket(this.dhtSocket);
    }

    public void start() {
        LOGGER.info(Markers.DHT, "Starting DHT");
        dhtSocket.start();
        dhtManager.start();
    }

    public void stop() {
        LOGGER.info(Markers.DHT, "Stopping DHT");
        dhtSocket.stop();
        dhtManager.stop();
    }

    public void addPeerDiscoveryListener(DhtManager.PeerDiscoveryListener peerDiscoveryListener) {
        dhtManager.addPeerDiscoveryListener(peerDiscoveryListener);
    }

    public void registerInfoHash(Sha1Hash infoHash) {
        dhtManager.registerInfoHash(infoHash);
    }

    public void deregisterInfoHash(Sha1Hash infoHash) {
        dhtManager.deregisterInfoHash(infoHash);
    }

    public void addBootstrapNodeAddress(InetSocketAddress address) {
        dhtManager.addBootstrapNodeAddress(address);
    }
}
