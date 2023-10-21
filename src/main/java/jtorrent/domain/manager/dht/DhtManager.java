package jtorrent.domain.manager.dht;

import static jtorrent.domain.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jtorrent.domain.manager.dht.task.BootstrapTask;
import jtorrent.domain.manager.dht.task.RefreshBucketTask;
import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.model.dht.routingtable.Bucket;
import jtorrent.domain.model.dht.routingtable.RoutingTable;
import jtorrent.domain.socket.DhtSocket;

public class DhtManager {

    public static final int K = 8;
    public static final int ALPHA = 3;
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(15);
    private static final Logger LOGGER = System.getLogger(DhtManager.class.getName());

    private final DhtSocket socket;
    private final RoutingTable routingTable;
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
    /**
     * Used to prevent multiple bootstrap tasks from running at the same time.
     */
    private final Semaphore bootstrapSemaphore = new Semaphore(1);

    public DhtManager(DhtSocket socket, RoutingTable routingTable) {
        this.socket = requireNonNull(socket);
        this.routingTable = requireNonNull(routingTable);
        Node.setDhtSocket(socket);
    }

    public void start() {
        LOGGER.log(Level.INFO, "[DHT] Starting DHT node handler");
        socket.start();
    }

    public void stop() {
        socket.stop();
    }

    public void addBootstrapNodeAddress(InetSocketAddress address) {
        requireNonNull(address);

        if (!routingTable.isEmpty()) {
            LOGGER.log(Level.DEBUG, "[DHT] Routing table is already bootstrapped");
            return;
        }

        if (!bootstrapSemaphore.tryAcquire()) {
            LOGGER.log(Level.DEBUG, "[DHT] Routing table is already bootstrapping");
            return;
        }

        Node.createNodeFromAddress(address)
                .thenApplyAsync(node -> new BootstrapTask(routingTable, node, cachedThreadPool), cachedThreadPool)
                .thenApply(BootstrapTask::getAsBoolean).whenComplete((isSuccess, throwable) -> {
                    if (throwable != null) {
                        LOGGER.log(Level.ERROR, "[DHT] Error occurred while bootstrapping)", throwable);
                    } else if (Boolean.FALSE.equals(isSuccess)) {
                        LOGGER.log(Level.ERROR,
                                "[DHT] Bootstrap node at {0} failed to provide new nodes. Routing table is empty", address);
                    } else {
                        LOGGER.log(Level.DEBUG,
                                "[DHT] Bootstrapped routing table with node at {0}." + " Routing table contains {1} nodes",
                                address, routingTable.size());
                    }
                    bootstrapSemaphore.release();
                    routingTable.getBuckets().forEach(this::startPeriodicallyRefreshingBucket);
                });
    }

    private void startPeriodicallyRefreshingBucket(Bucket bucket) {
        PeriodicRefreshBucketTask periodicRefreshBucketTask = new PeriodicRefreshBucketTask(bucket);
        scheduledThreadPool.schedule(periodicRefreshBucketTask, REFRESH_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private class PeriodicRefreshBucketTask implements Runnable {

        private final Bucket bucket;

        public PeriodicRefreshBucketTask(Bucket bucket) {
            this.bucket = requireNonNull(bucket);
        }

        @Override
        public void run() {
            LOGGER.log(Level.DEBUG, "[DHT] Starting periodic refresh for bucket {0}", bucket);
            Duration durationSinceLastUpdated = Duration.between(bucket.getLastUpdated(), LocalDateTime.now());
            Duration durationUntilNextUpdate = REFRESH_INTERVAL.minus(durationSinceLastUpdated);
            LOGGER.log(Level.DEBUG, "[DHT] Bucket {0} was last updated {1} ago", bucket, durationSinceLastUpdated);

            if (durationUntilNextUpdate.isZero() || durationUntilNextUpdate.isNegative()) {
                LOGGER.log(Level.DEBUG, "[DHT] Refreshing bucket {0} now", bucket);
                refresh().whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        LOGGER.log(Level.ERROR, "[DHT] Error occurred while refreshing bucket", throwable);
                    }
                    reschedule(REFRESH_INTERVAL);
                });
            } else {
                reschedule(durationUntilNextUpdate);
                LOGGER.log(Level.DEBUG, "[DHT] Checking bucket {0} again in {1}", bucket, durationUntilNextUpdate);
            }
        }

        private CompletableFuture<Void> refresh() {
            return CompletableFuture.runAsync(new RefreshBucketTask(routingTable, bucket), cachedThreadPool);
        }

        private void reschedule(Duration duration) {
            scheduledThreadPool.schedule(this, duration.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
