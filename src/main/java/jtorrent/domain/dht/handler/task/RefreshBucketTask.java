package jtorrent.domain.dht.handler.task;


import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.dht.handler.DhtManager;
import jtorrent.domain.dht.handler.lookup.FindNodeLookup;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.handler.routingtable.Bucket;
import jtorrent.domain.dht.handler.routingtable.RoutingTable;
import jtorrent.domain.dht.model.node.NodeId;

public class RefreshBucketTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshBucketTask.class);

    /**
     * The routing table that the bucket belongs to.
     */
    private final RoutingTable routingTable;
    /**
     * The bucket to refresh.
     */
    private final Bucket bucket;

    public RefreshBucketTask(RoutingTable routingTable, Bucket bucket) {
        this.routingTable = requireNonNull(routingTable);
        this.bucket = requireNonNull(bucket);
    }

    @Override
    public void run() {
        LOGGER.info(Markers.DHT, "Refreshing bucket {}", bucket);
        NodeId randomNodeId = NodeId.randomWithPrefix(bucket.getPrefix());
        Collection<Node> closestNodes = routingTable.getClosestNodes(randomNodeId, DhtManager.ALPHA);
        new FindNodeLookup().lookup(randomNodeId, closestNodes).forEach(routingTable::addNode);
        LOGGER.info(Markers.DHT, "Refreshed bucket {}", bucket);
    }
}
