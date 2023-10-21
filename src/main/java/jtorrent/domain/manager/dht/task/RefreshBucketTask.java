package jtorrent.domain.manager.dht.task;


import static jtorrent.domain.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.util.Collection;

import jtorrent.domain.manager.dht.DhtConstants;
import jtorrent.domain.manager.dht.lookup.FindNodeLookup;
import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.model.dht.routingtable.Bucket;
import jtorrent.domain.model.dht.routingtable.RoutingTable;

public class RefreshBucketTask implements Runnable {

    private static final Logger LOGGER = System.getLogger(RefreshBucketTask.class.getName());

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
        LOGGER.log(Logger.Level.DEBUG, "[DHT] Refreshing bucket {0}", bucket);
        NodeId randomNodeId = NodeId.randomWithPrefix(bucket.getPrefix());
        Collection<Node> closestNodes = routingTable.getClosestNodes(randomNodeId, DhtConstants.ALPHA);
        new FindNodeLookup().lookup(randomNodeId, closestNodes).forEach(routingTable::addNode);
        LOGGER.log(Logger.Level.DEBUG, "[DHT] Refreshed bucket {0}", bucket);
    }
}