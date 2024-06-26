package jtorrent.domain.dht.handler.lookup;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.Bit160Value;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.dht.handler.DhtManager;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.handler.util.DistanceToTargetComparator;
import jtorrent.domain.dht.model.message.response.Response;
import jtorrent.domain.dht.model.node.NodeContactInfo;

public abstract class IterativeLookup<T extends Response, U extends Bit160Value, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IterativeLookup.class);

    private U target;
    private NodeStore nodeStore;

    public R lookup(U target, Collection<Node> initialNodes) {
        this.target = requireNonNull(target);
        this.nodeStore = new NodeStore(target);
        requireNonNull(initialNodes).forEach(nodeStore::addNewNode);

        LOGGER.info(Markers.DHT, "Starting {} lookup for {}", getName(), getTarget());
        BigInteger prevMinDist = Bit160Value.MAX.toBigInteger();

        while (!nodeStore.isAllClosestNodesResponded()) {
            Collection<Node> nodesToQuery = getNodesToQuery(prevMinDist, nodeStore.getMinDist());
            Collection<T> responses = queryNodes(nodesToQuery);
            getNodesFromResponses(responses).forEach(nodeStore::addNewNode);
        }

        LOGGER.info(Markers.DHT, "Completed {} lookup for {}", getName(), getTarget());
        return getResult();
    }

    private Collection<Node> getNodesToQuery(BigInteger prevMinDist, BigInteger curMinDist) {
        boolean isCloserNodeFound = curMinDist.compareTo(prevMinDist) < 0;
        if (isCloserNodeFound) {
            return nodeStore.getClosestUnqueriedNodes(DhtManager.ALPHA);
        } else {
            return nodeStore.getClosestUnqueriedNodes();
        }
    }

    private Collection<T> queryNodes(Collection<Node> nodes) {
        List<T> responses = new ArrayList<>();
        List<CompletableFuture<Void>> futures = nodes.stream()
                .map(node -> queryNode(node)
                        .thenAccept(responses::add)
                        .exceptionally(throwable -> null))
                .collect(Collectors.toList());
        futures.forEach(CompletableFuture::join);
        return responses;
    }

    private Collection<Node> getNodesFromResponses(Collection<T> responses) {
        return responses.stream()
                .map(this::getNodesFromResponse)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    protected abstract R getResult();

    /**
     * Gets the name of the lookup. Used for logging purposes.
     *
     * @return name of the lookup
     */
    protected abstract String getName();

    protected Bit160Value getTarget() {
        return target;
    }

    private CompletableFuture<T> queryNode(Node node) {
        nodeStore.addQueriedNode(node);
        return doQuery(node, target)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        nodeStore.removeNodeFromConsideration(node);
                    } else {
                        nodeStore.addRespondedNode(node);
                    }
                });
    }

    protected abstract Collection<Node> getNodesFromResponse(T response);

    protected abstract CompletableFuture<T> doQuery(Node node, U target);

    protected Collection<Node> getClosestNodesSeen() {
        return nodeStore.getClosetNodes();
    }

    private static class NodeStore {

        private final Bit160Value target;
        /**
         * The K closest nodes to the target
         */
        private final Queue<Node> closestNodes;
        /**
         * The remaining nodes that are not among the K closest nodes to the target
         * and are used to replace the K closest nodes if they fail to respond.
         */
        private final Queue<Node> backupNodes;
        private final Set<NodeContactInfo> seenNodes = new HashSet<>();
        private final Set<NodeContactInfo> queriedNodes = new HashSet<>();
        private final Set<NodeContactInfo> respondedNodes = new HashSet<>();
        private final Set<NodeContactInfo> discardedNodes = new HashSet<>();
        private BigInteger minDist = Bit160Value.MAX.toBigInteger();

        public NodeStore(Bit160Value target) {
            this.target = requireNonNull(target);
            Comparator<Node> nodeComparator = new DistanceToTargetComparator(target);
            closestNodes = new PriorityBlockingQueue<>(DhtManager.K, nodeComparator.reversed());
            backupNodes = new PriorityQueue<>(DhtManager.K, nodeComparator);
        }

        public void addNewNode(Node node) {
            if (seenNodes.contains(node.getNodeContactInfo())) {
                return;
            }

            seenNodes.add(node.getNodeContactInfo());
            closestNodes.add(node);

            BigInteger distance = node.getId().distanceTo(target);
            if (distance.compareTo(minDist) < 0) {
                minDist = distance;
            }

            if (closestNodes.size() <= DhtManager.K) {
                return;
            }

            Node furthestNode = closestNodes.poll();

            backupNodes.add(furthestNode);
        }

        public void addRespondedNode(Node node) {
            respondedNodes.add(node.getNodeContactInfo());
        }

        public void addQueriedNode(Node node) {
            queriedNodes.add(node.getNodeContactInfo());
        }

        public Collection<Node> getClosetNodes() {
            return closestNodes;
        }

        public BigInteger getMinDist() {
            return minDist;
        }

        public boolean isAllClosestNodesResponded() {
            Set<NodeContactInfo> closestNodeContactInfos = closestNodes.stream()
                    .map(Node::getNodeContactInfo)
                    .collect(Collectors.toSet());
            return respondedNodes.containsAll(closestNodeContactInfos);
        }

        public Collection<Node> getClosestUnqueriedNodes() {
            return getClosestUnqueriedNodes(DhtManager.K);
        }

        public Collection<Node> getClosestUnqueriedNodes(int limit) {
            return closestNodes.stream()
                    .filter(node -> !queriedNodes.contains(node.getNodeContactInfo()))
                    .sorted(new DistanceToTargetComparator(target))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        public void removeNodeFromConsideration(Node node) {
            discardedNodes.add(node.getNodeContactInfo());

            if (!closestNodes.remove(node)) {
                return;
            }
            getClosestBackupNode().ifPresent(closestNodes::add);

            minDist = closestNodes.stream()
                    .map(Node::getId)
                    .map(nodeId -> nodeId.distanceTo(target))
                    .min(BigInteger::compareTo)
                    .orElse(Bit160Value.MAX.toBigInteger());
        }

        private Optional<Node> getClosestBackupNode() {
            while (!backupNodes.isEmpty()) {
                Node node = backupNodes.poll();
                if (!discardedNodes.contains(node.getNodeContactInfo())) {
                    return Optional.of(node);
                }
            }

            return Optional.empty();
        }
    }
}
