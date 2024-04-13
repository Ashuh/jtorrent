package jtorrent.domain.dht.handler;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.Bit160Value;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.dht.communication.DhtSocket;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.handler.routingtable.RoutingTable;
import jtorrent.domain.dht.model.message.query.AnnouncePeer;
import jtorrent.domain.dht.model.message.query.FindNode;
import jtorrent.domain.dht.model.message.query.GetPeers;
import jtorrent.domain.dht.model.message.query.Ping;
import jtorrent.domain.dht.model.node.NodeContactInfo;
import jtorrent.domain.peer.model.PeerContactInfo;

public class DhtQueryHandler implements DhtSocket.QueryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DhtQueryHandler.class);
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
        LOGGER.info(Markers.DHT, "Received ping from {}", nodeContactInfo);
        Node node = Node.seenNowWithContactInfo(nodeContactInfo);
        try {
            node.sendPingResponse();
            LOGGER.info(Markers.DHT, "Sent ping response to {}", nodeContactInfo);
        } catch (IOException e) {
            LOGGER.error(Markers.DHT, "Failed to send ping response to {}", nodeContactInfo, e);
        }
    }

    @Override
    public void handle(FindNode findNode, NodeContactInfo nodeContactInfo) {
        LOGGER.info(Markers.DHT, "Received find node from {}", nodeContactInfo);
        Node node = Node.seenNowWithContactInfo(nodeContactInfo);
        Collection<NodeContactInfo> closestNodes = getClosestNodes(findNode.getTarget());
        try {
            node.sendFindNodeResponse(closestNodes);
            LOGGER.info(Markers.DHT, "Sent find node response to {}", nodeContactInfo);
        } catch (IOException e) {
            LOGGER.error(Markers.DHT, "Failed to send find node response to {}", nodeContactInfo, e);
        }
    }

    @Override
    public void handle(GetPeers getPeers, NodeContactInfo nodeContactInfo) {
        LOGGER.info(Markers.DHT, "Received get peers from {}", nodeContactInfo);
        Node node = Node.seenNowWithContactInfo(nodeContactInfo);
        Collection<PeerContactInfo> peers = peerContactInfoStore.getPeerContactInfos(getPeers.getInfoHash());
        byte[] token = generateToken();
        tokenStore.add(nodeContactInfo, token, TOKEN_EXPIRATION_MINS, TimeUnit.MINUTES);

        if (!peers.isEmpty()) {
            LOGGER.debug(Markers.DHT, "Found {} peers for info hash {}", peers.size(), getPeers.getInfoHash());
            try {
                node.sendGetPeersResponseWithPeers(token, peers);
            } catch (IOException e) {
                LOGGER.error(Markers.DHT, "Failed to send get peers response to {}", nodeContactInfo, e);
            }
        } else {
            Collection<NodeContactInfo> closestNodes = getClosestNodes(getPeers.getInfoHash());
            LOGGER.debug(Markers.DHT, "No peers found for info hash {}. Found {} closes nodes", getPeers.getInfoHash(),
                    closestNodes.size());
            try {
                node.sendGetPeersResponseWithNodes(token, closestNodes);
            } catch (IOException e) {
                LOGGER.error(Markers.DHT, "Failed to send get peers response to {}", nodeContactInfo, e);
            }
        }
        LOGGER.info(Markers.DHT, "Sent get peers response to {}", nodeContactInfo);
    }

    @Override
    public void handle(AnnouncePeer announcePeer, NodeContactInfo nodeContactInfo) {
        LOGGER.info(Markers.DHT, "Received announce peer from {}", nodeContactInfo);
        Node node = Node.seenNowWithContactInfo(nodeContactInfo);

        byte[] token = tokenStore.getToken(nodeContactInfo);
        if (token == null || !Arrays.equals(token, announcePeer.getToken())) {
            LOGGER.error(Markers.DHT, "Invalid token for announce peer from {}", nodeContactInfo);
            return;
        }

        InetAddress address = node.getAddress();
        int port = announcePeer.getPort();
        peerContactInfoStore.addPeerContactInfo(announcePeer.getInfoHash(), new PeerContactInfo(address, port));
        LOGGER.info(Markers.DHT, "Added peer contact info for info hash {}", announcePeer.getInfoHash());

        try {
            node.sendAnnouncePeerResponse();
            LOGGER.info(Markers.DHT, "Sent announce peer response to {}", nodeContactInfo);
        } catch (IOException e) {
            LOGGER.error(Markers.DHT, "Failed to send announce peer response to {}", nodeContactInfo, e);
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
