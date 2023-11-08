package jtorrent.dht.domain.handler;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import jtorrent.dht.domain.handler.node.Node;
import jtorrent.dht.domain.handler.routingtable.RoutingTable;
import jtorrent.dht.domain.communication.DhtSocket;
import jtorrent.common.domain.util.Sha1Hash;

public class DhtClient {

    private static final Logger LOGGER = System.getLogger(DhtClient.class.getName());

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
        LOGGER.log(Level.INFO, "[DHT] Starting DHT");
        dhtSocket.start();
        dhtManager.start();
    }

    public void stop() {
        LOGGER.log(Level.INFO, "[DHT] Stopping DHT");
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
