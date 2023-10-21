package jtorrent.domain.manager.dht.task;

import static jtorrent.domain.util.ValidationUtil.requireNonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

import jtorrent.domain.manager.dht.lookup.FindNodeLookup;
import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.model.dht.routingtable.RoutingTable;

public class BootstrapTask implements BooleanSupplier {

    private final RoutingTable routingTable;
    private final Node bootstrapNode;
    private final Executor executor;

    /**
     * @param routingTable  The {@link RoutingTable} to populate
     * @param bootstrapNode The {@link Node} to bootstrap from
     * @param executor      The {@link Executor} to use to run the lookup
     */
    public BootstrapTask(RoutingTable routingTable, Node bootstrapNode, Executor executor) {
        this.routingTable = requireNonNull(routingTable);
        this.bootstrapNode = requireNonNull(bootstrapNode);
        this.executor = requireNonNull(executor);
    }

    /**
     * Runs the bootstrap task.
     *
     * @return {@code true} if the bootstrap task was successful, {@code false} otherwise
     */
    @Override
    public boolean getAsBoolean() {
        return bootstrap();
    }

    private boolean bootstrap() {
        if (!findClosestNodes()) {
            // No nodes were found, so the bootstrap node's routing table is empty and there is no point in continuing
            return false;
        }

        refreshEmptyBucketsFurtherThanNonEmptyBuckets();
        return true;
    }

    /**
     * Finds the closest nodes to the local node and adds them to the routing table.
     * <p>
     * If no nodes are found, nothing is added to the routing table.
     * Otherwise, the nodes found and the bootstrap node are added to the routing table
     *
     * @return {@code true} if nodes were found, {@code false} otherwise
     */
    private boolean findClosestNodes() {
        new FindNodeLookup()
                .lookup(NodeId.LOCAL, List.of(bootstrapNode))
                .forEach(routingTable::addNode);

        if (routingTable.isEmpty()) {
            return false;
        }

        routingTable.addNode(bootstrapNode);
        return true;
    }

    private void refreshEmptyBucketsFurtherThanNonEmptyBuckets() {
        CompletableFuture<?>[] futures = routingTable.getEmptyBucketsFurtherThanNonEmptyBuckets().stream()
                .map(bucket -> new RefreshBucketTask(routingTable, bucket, new FindNodeLookup()))
                .map(refreshBucketTask -> CompletableFuture.runAsync(refreshBucketTask, executor))
                .toArray(CompletableFuture<?>[]::new);
        CompletableFuture.allOf(futures).join();
    }
}
