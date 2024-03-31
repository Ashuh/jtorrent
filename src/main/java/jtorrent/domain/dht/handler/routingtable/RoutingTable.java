package jtorrent.domain.dht.handler.routingtable;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import jtorrent.domain.common.util.Bit160Value;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.handler.util.DistanceToTargetComparator;
import jtorrent.domain.dht.model.node.NodeId;

public class RoutingTable {

    private static final Logger LOGGER = System.getLogger(RoutingTable.class.getName());
    private static final int NUM_BUCKETS = 160;

    private final Bucket[] buckets = new Bucket[160];

    public RoutingTable() {
        for (int i = 0; i < NUM_BUCKETS; i++) {
            buckets[i] = new Bucket(NodeId.LOCAL.getPrefix(i));
        }
    }

    public Collection<Bucket> getEmptyBucketsFurtherThanNonEmptyBuckets() {
        Collection<Bucket> result = new ArrayList<>();

        for (int i = NUM_BUCKETS - 1; i >= 0; i--) {
            Bucket bucket = getBucket(i);
            if (!bucket.isEmpty()) {
                break;
            }
            result.add(bucket);
        }

        return result;
    }

    public Bucket getBucket(int numMatchingPrefixBits) {
        return buckets[numMatchingPrefixBits];
    }

    public Collection<Bucket> getBuckets() {
        return Arrays.asList(buckets);
    }

    public Collection<Node> getClosestNodes(Bit160Value target, int limit) {
        return Arrays.stream(buckets)
                .flatMap(bucket -> bucket.getNodes().stream())
                .sorted(new DistanceToTargetComparator(target))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean addNode(Node node) {
        boolean isAdded = getBucketForNode(node).addNode(node);

        if (isAdded) {
            LOGGER.log(Level.DEBUG, "[DHT] Added Node {0} to routing table", node);
        } else {
            LOGGER.log(Level.DEBUG, "[DHT] Failed to add Node {0} to routing table", node);
        }

        return isAdded;
    }

    private Bucket getBucketForNode(Node node) {
        int numMatchingBits = node.getId().numMatchingBits(NodeId.LOCAL);
        return buckets[numMatchingBits];
    }

    public boolean isEmpty() {
        return Arrays.stream(buckets).allMatch(Bucket::isEmpty);
    }

    public int size() {
        return Arrays.stream(buckets).mapToInt(Bucket::size).sum();
    }
}
