package jtorrent.dht.domain.handler;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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

import jtorrent.common.domain.Constants;
import jtorrent.common.domain.util.PeriodicTask;
import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.dht.domain.handler.lookup.GetPeersLookup;
import jtorrent.dht.domain.handler.node.Node;
import jtorrent.dht.domain.handler.routingtable.Bucket;
import jtorrent.dht.domain.handler.routingtable.RoutingTable;
import jtorrent.dht.domain.handler.task.BootstrapTask;
import jtorrent.dht.domain.handler.task.RefreshBucketTask;
import jtorrent.peer.domain.model.PeerContactInfo;

public class DhtManager {

    public static final int K = 8;
    public static final int ALPHA = 3;
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(15);
    private static final Logger LOGGER = System.getLogger(DhtManager.class.getName());

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
                                "[DHT] Bootstrap node at {0} failed to provide new nodes. Routing table is empty",
                                address);
                    } else {
                        LOGGER.log(Level.DEBUG,
                                "[DHT] Bootstrapped routing table with node at {0}."
                                        + " Routing table contains {1} nodes",
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

    /**
     * Registers the given info hash to be periodically searched for peers.
     *
     * @param infoHash the info hash to register
     */
    public void registerInfoHash(Sha1Hash infoHash) {
        if (infoHashToFindPeersTask.containsKey(infoHash)) {
            LOGGER.log(Level.DEBUG, "[DHT] Already registered info hash {0}", infoHash);
            return;
        }

        PeriodicFindPeersTask periodicFindPeersTask = new PeriodicFindPeersTask(infoHash);
        infoHashToFindPeersTask.put(infoHash, periodicFindPeersTask);
        periodicFindPeersTask.scheduleWithFixedDelay(1, TimeUnit.MINUTES);
        LOGGER.log(Level.DEBUG, "[DHT] Registered info hash {0}", infoHash);
    }

    public void deregisterInfoHash(Sha1Hash infoHash) {
        PeriodicFindPeersTask periodicFindPeersTask = infoHashToFindPeersTask.remove(infoHash);
        if (periodicFindPeersTask != null) {
            LOGGER.log(Level.DEBUG, "[DHT] Stopping periodic find peers task for info hash {0}", infoHash);
            periodicFindPeersTask.stop();
        }
        LOGGER.log(Level.DEBUG, "[DHT] Deregistered info hash {0}", infoHash);
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
