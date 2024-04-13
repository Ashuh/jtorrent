package jtorrent.domain.dht.handler.routingtable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.Bit160Value;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.handler.util.DistanceToTargetComparator;
import jtorrent.domain.dht.model.node.NodeId;

public class RoutingTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingTable.class);
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
            LOGGER.debug(Markers.DHT, "Added Node {} to routing table", node);
        } else {
            LOGGER.debug(Markers.DHT, "Failed to add Node {} to routing table", node);
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
