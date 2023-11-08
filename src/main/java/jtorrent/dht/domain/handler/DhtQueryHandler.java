package jtorrent.dht.domain.handler;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jtorrent.dht.domain.handler.node.Node;
import jtorrent.dht.domain.handler.routingtable.RoutingTable;
import jtorrent.dht.domain.model.message.query.AnnouncePeer;
import jtorrent.dht.domain.model.message.query.FindNode;
import jtorrent.dht.domain.model.message.query.GetPeers;
import jtorrent.dht.domain.model.message.query.Ping;
import jtorrent.dht.domain.model.node.NodeContactInfo;
import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.dht.domain.communication.DhtSocket;
import jtorrent.common.domain.util.Bit160Value;

public class DhtQueryHandler implements DhtSocket.QueryHandler {

    private static final Logger LOGGER = System.getLogger(DhtQueryHandler.class.getName());
    private static final int TOKEN_LENGTH_BYTES = 20;
    private static final int TOKEN_EXPIRATION_MINS = 10;

    private final RoutingTable routingTable;
    private final PeerContactInfoStore peerContactInfoStore;
    private final TokenStore tokenStore = new TokenStore();

    public DhtQueryHandler(RoutingTable routingTable, PeerContactInfoStore peerContactInfoStore) {
        this.routingTable = requireNonNull(routingTable);
        this.peerContactInfoStore = requireNonNull(peerContactInfoStore);
    }

    @Override
    public void handle(Ping ping, NodeContactInfo nodeContactInfo) {
        LOGGER.log(Level.DEBUG, "[DHT] Handling ping from {0}", nodeContactInfo);
        Node node = Node.seenNowWithContactInfo(nodeContactInfo);
        try {
            node.sendPingResponse();
            LOGGER.log(Level.DEBUG, "[DHT] Sent ping response to {0}", nodeContactInfo);
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "[DHT] Failed to send ping response to {0}", nodeContactInfo);
        }
    }

    @Override
    public void handle(FindNode findNode, NodeContactInfo nodeContactInfo) {
        LOGGER.log(Level.DEBUG, "[DHT] Handling find node from {0}", nodeContactInfo);
        Node node = Node.seenNowWithContactInfo(nodeContactInfo);
        Collection<NodeContactInfo> closestNodes = getClosestNodes(findNode.getTarget());
        try {
            node.sendFindNodeResponse(closestNodes);
            LOGGER.log(Level.DEBUG, "[DHT] Sent find node response to {0}", nodeContactInfo);
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "[DHT] Failed to send find node response to {0}", nodeContactInfo);
        }
    }

    @Override
    public void handle(GetPeers getPeers, NodeContactInfo nodeContactInfo) {
        LOGGER.log(Level.DEBUG, "[DHT] Handling get peers from {0}", nodeContactInfo);
        Node node = Node.seenNowWithContactInfo(nodeContactInfo);
        Collection<PeerContactInfo> peers = peerContactInfoStore.getPeerContactInfos(getPeers.getInfoHash());
        byte[] token = generateToken();
        tokenStore.add(nodeContactInfo, token, TOKEN_EXPIRATION_MINS, TimeUnit.MINUTES);

        if (!peers.isEmpty()) {
            LOGGER.log(Level.DEBUG, "[DHT] Found peers for info hash {0}", getPeers.getInfoHash());
            try {
                node.sendGetPeersResponseWithPeers(token, peers);
                LOGGER.log(Level.DEBUG, "[DHT] Sent get peers response to {0}", nodeContactInfo);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[DHT] Failed to send get peers response to {0}", nodeContactInfo);
            }
        } else {
            LOGGER.log(Level.DEBUG, "[DHT] No peers found for info hash {0}", getPeers.getInfoHash());
            Collection<NodeContactInfo> closestNodes = getClosestNodes(getPeers.getInfoHash());
            try {
                node.sendGetPeersResponseWithNodes(token, closestNodes);
                LOGGER.log(Level.DEBUG, "[DHT] Sent get peers response to {0}", nodeContactInfo);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[DHT] Failed to send get peers response to {0}", nodeContactInfo);
            }
        }
    }

    @Override
    public void handle(AnnouncePeer announcePeer, NodeContactInfo nodeContactInfo) {
        LOGGER.log(Level.DEBUG, "[DHT] Handling announce peer from {0}", nodeContactInfo);
        Node node = Node.seenNowWithContactInfo(nodeContactInfo);

        byte[] token = tokenStore.getToken(nodeContactInfo);
        if (token == null || !Arrays.equals(token, announcePeer.getToken())) {
            LOGGER.log(Level.ERROR, "[DHT] Invalid token for announce peer from {0}", nodeContactInfo);
            return;
        }

        InetAddress address = node.getAddress();
        int port = announcePeer.getPort();
        peerContactInfoStore.addPeerContactInfo(announcePeer.getInfoHash(), new PeerContactInfo(address, port));
        LOGGER.log(Level.DEBUG, "[DHT] Added peer contact info for info hash {0}", announcePeer.getInfoHash());

        try {
            node.sendAnnouncePeerResponse();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "[DHT] Failed to send announce peer response to {0}", nodeContactInfo);
        }
    }

    private static byte[] generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(token);
        return token;
    }

    private Collection<NodeContactInfo> getClosestNodes(Bit160Value target) {
        return routingTable.getClosestNodes(target, DhtManager.K)
                .stream()
                .map(Node::getNodeContactInfo)
                .collect(Collectors.toList());
    }

    private static class TokenStore {

        private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    return thread;
                });

        private final Map<NodeContactInfo, byte[]> nodeContactInfoToToken = new ConcurrentHashMap<>();

        public void add(NodeContactInfo nodeContactInfo, byte[] token, long expiration, TimeUnit timeUnit) {
            nodeContactInfoToToken.put(nodeContactInfo, token);
            SCHEDULED_EXECUTOR_SERVICE.schedule(() -> remove(nodeContactInfo), expiration, timeUnit);
        }

        public void remove(NodeContactInfo nodeContactInfo) {
            nodeContactInfoToToken.remove(nodeContactInfo);
        }

        public byte[] getToken(NodeContactInfo nodeContactInfo) {
            return nodeContactInfoToToken.get(nodeContactInfo);
        }
    }
}
