package jtorrent.domain.manager.dht;

import static jtorrent.domain.util.ValidationUtil.requireNonNull;

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

import jtorrent.domain.model.dht.message.query.AnnouncePeer;
import jtorrent.domain.model.dht.message.query.FindNode;
import jtorrent.domain.model.dht.message.query.GetPeers;
import jtorrent.domain.model.dht.message.query.Ping;
import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.model.dht.node.NodeContactInfo;
import jtorrent.domain.model.dht.routingtable.RoutingTable;
import jtorrent.domain.model.peer.OutgoingPeer;
import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.socket.DhtSocket;
import jtorrent.domain.util.Bit160Value;

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
    public void handle(Ping ping, Node node) {
        LOGGER.log(Level.DEBUG, "[DHT] Handling ping from {0}", node);
        try {
            node.sendPingResponse();
            LOGGER.log(Level.DEBUG, "[DHT] Sent ping response to {0}", node);
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "[DHT] Failed to send ping response to {0}", node);
        }
    }

    @Override
    public void handle(FindNode findNode, Node node) {
        LOGGER.log(Level.DEBUG, "[DHT] Handling find node from {0}", node);
        Collection<NodeContactInfo> closestNodes = getClosestNodes(findNode.getTarget());
        try {
            node.sendFindNodeResponse(closestNodes);
            LOGGER.log(Level.DEBUG, "[DHT] Sent find node response to {0}", node);
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "[DHT] Failed to send find node response to {0}", node);
        }
    }

    @Override
    public void handle(GetPeers getPeers, Node node) {
        LOGGER.log(Level.DEBUG, "[DHT] Handling get peers from {0}", node);

        Collection<Peer> peers = peerContactInfoStore.getPeerContactInfos(getPeers.getInfoHash());
        byte[] token = generateToken();
        tokenStore.add(node.getNodeContactInfo(), token, TOKEN_EXPIRATION_MINS, TimeUnit.MINUTES);

        if (!peers.isEmpty()) {
            LOGGER.log(Level.DEBUG, "[DHT] Found peers for info hash {0}", getPeers.getInfoHash());
            try {
                node.sendGetPeersResponseWithPeers(token, peers);
                LOGGER.log(Level.DEBUG, "[DHT] Sent get peers response to {0}", node);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[DHT] Failed to send get peers response to {0}", node);
            }
        } else {
            LOGGER.log(Level.DEBUG, "[DHT] No peers found for info hash {0}", getPeers.getInfoHash());
            Collection<NodeContactInfo> closestNodes = getClosestNodes(getPeers.getInfoHash());
            try {
                node.sendGetPeersResponseWithNodes(token, closestNodes);
                LOGGER.log(Level.DEBUG, "[DHT] Sent get peers response to {0}", node);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[DHT] Failed to send get peers response to {0}", node);
            }
        }
    }

    @Override
    public void handle(AnnouncePeer announcePeer, Node node) {
        LOGGER.log(Level.DEBUG, "[DHT] Handling announce peer from {0}", node);

        byte[] token = tokenStore.getToken(node.getNodeContactInfo());
        if (token == null || !Arrays.equals(token, announcePeer.getToken())) {
            LOGGER.log(Level.ERROR, "[DHT] Invalid token for announce peer from {0}", node);
            return;
        }

        InetAddress address = node.getAddress();
        int port = announcePeer.getPort();
        peerContactInfoStore.addPeerContactInfo(announcePeer.getInfoHash(), new OutgoingPeer(address, port));
        LOGGER.log(Level.DEBUG, "[DHT] Added peer contact info for info hash {0}", announcePeer.getInfoHash());
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
