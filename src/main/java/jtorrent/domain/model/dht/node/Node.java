package jtorrent.domain.model.dht.node;

import static jtorrent.domain.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import jtorrent.domain.model.dht.message.query.AnnouncePeer;
import jtorrent.domain.model.dht.message.query.FindNode;
import jtorrent.domain.model.dht.message.query.GetPeers;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.message.query.Ping;
import jtorrent.domain.model.dht.message.response.AnnouncePeerResponse;
import jtorrent.domain.model.dht.message.response.DefinedResponse;
import jtorrent.domain.model.dht.message.response.FindNodeResponse;
import jtorrent.domain.model.dht.message.response.GetPeersResponse;
import jtorrent.domain.model.dht.message.response.PingResponse;
import jtorrent.domain.socket.DhtSocket;
import jtorrent.domain.util.Bit160Value;
import jtorrent.domain.util.Sha1Hash;

public class Node {

    private static final Logger LOGGER = System.getLogger(Node.class.getName());
    private static final Map<NodeContactInfo, WeakReference<Node>> NODE_CONTACT_INFO_TO_NODE =
            new ConcurrentHashMap<>();
    private static DhtSocket dhtSocket;

    private final NodeContactInfo nodeContactInfo;
    private LocalDateTime lastSeen;
    private int numFailedQueries;

    private Node(NodeContactInfo nodeContactInfo, LocalDateTime lastSeen) {
        this.nodeContactInfo = requireNonNull(nodeContactInfo);
        this.lastSeen = requireNonNull(lastSeen);
    }

    /**
     * Creates a {@link Node} from the given {@link InetSocketAddress} if it is contactable.
     *
     * @param address the address of the node
     * @return a {@link CompletableFuture} that will be completed with the node if it is contactable.
     * Otherwise, the future will be completed with an exception.
     */
    public static CompletableFuture<Node> createNodeFromAddress(InetSocketAddress address) {
        return dhtSocket.sendPing(new Ping(NodeId.LOCAL), address)
                .thenApply(PingResponse::getId)
                .thenApply(nodeId -> new NodeContactInfo(nodeId, address))
                .thenApply(Node::seenNowWithContactInfo);
    }

    public static Node seenNowWithContactInfo(NodeContactInfo nodeContactInfo) {
        Node node = withContactInfo(nodeContactInfo);
        node.setLastSeenNow();
        return node;
    }

    public static Node withContactInfo(NodeContactInfo nodeContactInfo) {
        WeakReference<Node> nodeReference = NODE_CONTACT_INFO_TO_NODE.get(nodeContactInfo);
        if (nodeReference == null || nodeReference.get() == null) {
            Node node = new Node(nodeContactInfo, LocalDateTime.MIN);
            NODE_CONTACT_INFO_TO_NODE.put(nodeContactInfo, new WeakReference<>(node));
            return node;
        } else {
            return nodeReference.get();
        }
    }

    private void setLastSeenNow() {
        lastSeen = LocalDateTime.now();
    }

    public static void setDhtSocket(DhtSocket dhtSocket) {
        if (Node.dhtSocket != null) {
            throw new IllegalStateException("[DHT] DhtSocket already set");
        }
        Node.dhtSocket = requireNonNull(dhtSocket);
    }

    public NodeContactInfo geNodeContactInfo() {
        return nodeContactInfo;
    }

    public NodeId getId() {
        return nodeContactInfo.getId();
    }

    public NodeContactInfo getNodeContactInfo() {
        return nodeContactInfo;
    }

    public Duration getDurationSinceLastSeen() {
        if (lastSeen == null) {
            return Duration.ofDays(Long.MAX_VALUE);
        }
        return Duration.between(lastSeen, LocalDateTime.now());
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public State getState() {
        if (numFailedQueries >= 3) {
            return State.BAD;
        }
        if (!isSeenWithin(Duration.ofMinutes(15))) {
            return State.QUESTIONABLE;
        }
        return State.GOOD;
    }

    public boolean isSeenWithin(Duration duration) {
        return lastSeen != null && lastSeen.plus(duration).isAfter(LocalDateTime.now());
    }

    private void incrementNumFailedQueries() {
        numFailedQueries++;
    }

    private void resetNumFailedQueries() {
        numFailedQueries = 0;
    }

    public boolean isContactable() {
        return isContactableAsync().join();
    }

    public CompletableFuture<Boolean> isContactableAsync() {
        return ping().handle((response, throwable) -> throwable == null);
    }

    public CompletableFuture<PingResponse> ping() {
        checkSocketIsSet();
        return dhtSocket.sendPing(new Ping(NodeId.LOCAL), getAddress())
                .whenComplete(new FutureHandler<>(Method.PING));
    }

    private static void checkSocketIsSet() {
        if (dhtSocket == null) {
            throw new IllegalStateException("[DHT] DhtSocket not set");
        }
    }

    public InetSocketAddress getAddress() {
        return nodeContactInfo.getAddress();
    }

    public CompletableFuture<FindNodeResponse> findNode(Bit160Value target) {
        checkSocketIsSet();
        FindNode findNode = new FindNode(NodeId.LOCAL, target);
        return dhtSocket.sendFindNode(findNode, getAddress())
                .whenComplete(new FutureHandler<>(Method.FIND_NODE));
    }

    public CompletableFuture<GetPeersResponse> getPeers(Sha1Hash infoHash) {
        checkSocketIsSet();
        GetPeers getPeers = new GetPeers(NodeId.LOCAL, infoHash);
        return dhtSocket.sendGetPeers(getPeers, getAddress())
                .whenComplete(new FutureHandler<>(Method.GET_PEERS));
    }

    public CompletableFuture<AnnouncePeerResponse> announcePeer(Sha1Hash infoHash, int port, String token) {
        checkSocketIsSet();
        AnnouncePeer announcePeer = new AnnouncePeer(NodeId.LOCAL, infoHash, port, token);
        return dhtSocket.sendAnnouncePeer(announcePeer, getAddress())
                .whenComplete(new FutureHandler<>(Method.ANNOUNCE_PEER));
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeContactInfo, lastSeen);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node that = (Node) o;
        return Objects.equals(nodeContactInfo, that.nodeContactInfo) && Objects.equals(lastSeen, that.lastSeen);
    }

    @Override
    public String toString() {
        return "Node{"
                + "nodeContactInfo=" + nodeContactInfo
                + '}';
    }

    public enum State {

        GOOD,
        QUESTIONABLE,
        BAD
    }

    private class FutureHandler<R extends DefinedResponse> implements BiConsumer<R, Throwable> {

        private final Method queryMethod;

        public FutureHandler(Method queryMethod) {
            this.queryMethod = requireNonNull(queryMethod);
        }

        @Override
        public void accept(R response, Throwable throwable) {
            if (throwable != null) {
                LOGGER.log(Level.ERROR, "[DHT] {0} query to {1} failed: {2}", queryMethod, this, throwable);
                incrementNumFailedQueries();
            } else {
                LOGGER.log(Level.DEBUG, "[DHT] {0} query to {1} succeeded: {2}", queryMethod, this, response);
                resetNumFailedQueries();
                setLastSeenNow();
            }
        }
    }
}
