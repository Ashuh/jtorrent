package jtorrent.domain.dht.handler.task;


import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;

import jtorrent.domain.dht.handler.DhtManager;
import jtorrent.domain.dht.handler.lookup.FindNodeLookup;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.handler.routingtable.Bucket;
import jtorrent.domain.dht.handler.routingtable.RoutingTable;
import jtorrent.domain.dht.model.node.NodeId;

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
        LOGGER.log(Level.DEBUG, "[DHT] Refreshing bucket {0}", bucket);
        NodeId randomNodeId = NodeId.randomWithPrefix(bucket.getPrefix());
        Collection<Node> closestNodes = routingTable.getClosestNodes(randomNodeId, DhtManager.ALPHA);
        new FindNodeLookup().lookup(randomNodeId, closestNodes).forEach(routingTable::addNode);
        LOGGER.log(Level.DEBUG, "[DHT] Refreshed bucket {0}", bucket);
    }
}
