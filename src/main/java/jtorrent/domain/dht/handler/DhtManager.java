package jtorrent.domain.dht.handler;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.Constants;
import jtorrent.domain.common.util.PeriodicTask;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.dht.handler.lookup.GetPeersLookup;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.handler.routingtable.Bucket;
import jtorrent.domain.dht.handler.routingtable.RoutingTable;
import jtorrent.domain.dht.handler.task.BootstrapTask;
import jtorrent.domain.dht.handler.task.RefreshBucketTask;
import jtorrent.domain.peer.model.PeerContactInfo;

public class DhtManager {

    public static final int K = 8;
    public static final int ALPHA = 3;
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(15);
    private static final Logger LOGGER = LoggerFactory.getLogger(DhtManager.class);

    private final RoutingTable routingTable;
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
    /**
     * Used to prevent multiple bootstrap tasks from running at the same time.
     */
    private final Semaphore bootstrapSemaphore = new Semaphore(1);
    private final Map<Sha1Hash, PeriodicFindPeersTask> infoHashToFindPeersTask = new ConcurrentHashMap<>();
    private final List<PeerDiscoveryListener> peerDiscoveryListeners = new ArrayList<>();

    public DhtManager(RoutingTable routingTable) {
        this.routingTable = requireNonNull(routingTable);
    }

    public void start() {
    }

    public void stop() {
        scheduledThreadPool.shutdownNow();
    }

    public void addPeerDiscoveryListener(PeerDiscoveryListener peerDiscoveryListener) {
        peerDiscoveryListeners.add(peerDiscoveryListener);
    }

    public void addBootstrapNodeAddress(InetSocketAddress address) {
        requireNonNull(address);

        if (!routingTable.isEmpty()) {
            LOGGER.debug(Markers.DHT, "Routing table is already bootstrapped");
            return;
        }

        if (!bootstrapSemaphore.tryAcquire()) {
            LOGGER.debug(Markers.DHT, "Routing table is already bootstrapping");
            return;
        }

        Node.createNodeFromAddress(address)
                .thenApplyAsync(node -> new BootstrapTask(routingTable, node, cachedThreadPool), cachedThreadPool)
                .thenApply(BootstrapTask::getAsBoolean).whenComplete((isSuccess, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error(Markers.DHT, "Error occurred while bootstrapping", throwable);
                    } else if (Boolean.FALSE.equals(isSuccess)) {
                        LOGGER.error(Markers.DHT,
                                "Failed to bootstrap routing table: Bootstrap node {} failed to provide any new nodes",
                                address);
                    } else {
                        LOGGER.info(Markers.DHT, "Routing table bootstrapped with {}: {} nodes found", address,
                                routingTable.size());
                    }
                    bootstrapSemaphore.release();
                    routingTable.getBuckets().forEach(this::startPeriodicallyRefreshingBucket);
                });
    }

    private void startPeriodicallyRefreshingBucket(Bucket bucket) {
        PeriodicRefreshBucketTask periodicRefreshBucketTask = new PeriodicRefreshBucketTask(bucket);
        scheduledThreadPool.schedule(periodicRefreshBucketTask, REFRESH_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Registers the given info hash to be periodically searched for peers.
     *
     * @param infoHash the info hash to register
     */
    public void registerInfoHash(Sha1Hash infoHash) {
        if (infoHashToFindPeersTask.containsKey(infoHash)) {
            LOGGER.debug(Markers.DHT, "Info hash already registered: {}", infoHash);
            return;
        }

        PeriodicFindPeersTask periodicFindPeersTask = new PeriodicFindPeersTask(infoHash);
        infoHashToFindPeersTask.put(infoHash, periodicFindPeersTask);
        periodicFindPeersTask.scheduleWithFixedDelay(1, TimeUnit.MINUTES);
        LOGGER.info(Markers.DHT, "Registered info hash: {}", infoHash);
    }

    public void deregisterInfoHash(Sha1Hash infoHash) {
        PeriodicFindPeersTask periodicFindPeersTask = infoHashToFindPeersTask.remove(infoHash);
        if (periodicFindPeersTask != null) {
            LOGGER.debug(Markers.DHT, "Stopping periodic find peers task for info hash: {}", infoHash);
            periodicFindPeersTask.stop();
        }
        LOGGER.info(Markers.DHT, "Deregistered info hash: {}", infoHash);
    }

    public interface PeerDiscoveryListener {

        void onPeersDiscovered(Sha1Hash infoHash, Collection<PeerContactInfo> peers);
    }

    private class PeriodicRefreshBucketTask implements Runnable {

        private final Bucket bucket;

        public PeriodicRefreshBucketTask(Bucket bucket) {
            this.bucket = requireNonNull(bucket);
        }

        @Override
        public void run() {
            LOGGER.debug(Markers.DHT, "Starting periodic refresh check for bucket {}", bucket);
            Duration durationSinceLastUpdated = Duration.between(bucket.getLastUpdated(), LocalDateTime.now());
            Duration durationUntilNextUpdate = REFRESH_INTERVAL.minus(durationSinceLastUpdated);
            LOGGER.debug(Markers.DHT, "Bucket {} was last updated {} ago", bucket, durationSinceLastUpdated);

            if (durationUntilNextUpdate.isZero() || durationUntilNextUpdate.isNegative()) {
                refresh().whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error(Markers.DHT, "Failed to refresh bucket {}", bucket, throwable);
                    }
                    reschedule(REFRESH_INTERVAL);
                });
            } else {
                reschedule(durationUntilNextUpdate);
                LOGGER.debug(Markers.DHT, "Checking bucket {} again in {}", bucket, durationUntilNextUpdate);
            }
        }

        private CompletableFuture<Void> refresh() {
            return CompletableFuture.runAsync(new RefreshBucketTask(routingTable, bucket), cachedThreadPool);
        }

        private void reschedule(Duration duration) {
            scheduledThreadPool.schedule(this, duration.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private class PeriodicFindPeersTask extends PeriodicTask {

        private final Sha1Hash target;

        private PeriodicFindPeersTask(Sha1Hash target) {
            super(DhtManager.this.scheduledThreadPool);
            this.target = target;
        }

        @Override
        public void run() {
            Collection<Node> closestNodes = routingTable.getClosestNodes(target, DhtManager.ALPHA);
            GetPeersLookup.Result result = new GetPeersLookup().lookup(target, closestNodes);

            if (result.getPeers().isEmpty()) {
                return;
            }

            peerDiscoveryListeners.forEach(listener -> listener.onPeersDiscovered(target, result.getPeers()));
            result.getNodeToToken().forEach((node, token) -> node.announcePeer(target, Constants.PORT, token));
        }
    }
}
