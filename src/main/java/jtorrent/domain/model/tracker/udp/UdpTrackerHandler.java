package jtorrent.domain.model.tracker.udp;

import static jtorrent.domain.Constants.PEER_ID;
import static jtorrent.domain.Constants.PORT;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jtorrent.domain.model.torrent.Sha1Hash;
import jtorrent.domain.model.tracker.udp.exception.ExceededMaxTriesException;
import jtorrent.domain.model.tracker.udp.message.AnnounceRequest;
import jtorrent.domain.model.tracker.udp.message.AnnounceResponse;
import jtorrent.domain.model.tracker.udp.message.ConnectionRequest;
import jtorrent.domain.model.tracker.udp.message.ConnectionResponse;
import jtorrent.domain.model.tracker.udp.message.Event;
import jtorrent.domain.model.tracker.udp.message.PeerResponse;

public class UdpTrackerHandler implements Runnable {

    private static final Logger LOGGER = System.getLogger(UdpTrackerHandler.class.getName());
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    private static final int CONNECTION_ID_EXPIRATION_MINS = 1;
    private static final int TIMEOUT_MILLIS = 15000;
    private static final int MAX_TRIES = 8;

    private final UdpTracker tracker;
    private final Sha1Hash infoHash;
    private final List<Listener> listeners = new ArrayList<>();
    private final AnnounceTask announceTask = new AnnounceTask();
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private ScheduledFuture<AnnounceResponse> responseScheduledFuture;
    private boolean isRunning = true;
    private boolean isActive = true;

    public UdpTrackerHandler(UdpTracker tracker, Sha1Hash infoHash) {
        this.tracker = tracker;
        this.infoHash = infoHash;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void run() {
        try {
            tracker.init();
        } catch (SocketException e) {
            LOGGER.log(Level.ERROR, "Failed to initialize tracker", e);
            return;
        }

        scheduleAnnounce(0);

        while (isRunning) {
            waitForActiveState();

            try {
                LOGGER.log(Level.TRACE, "Waiting for announce to execute");
                AnnounceResponse announceResponse = responseScheduledFuture.get();
                LOGGER.log(Level.TRACE, "Received announce response");
                listeners.forEach(listener -> listener.onAnnounceResponse(announceResponse.getPeers()));
                int interval = announceResponse.getInterval();
                scheduleAnnounce(interval);
            } catch (InterruptedException e) {
                LOGGER.log(Level.ERROR, "Announce interrupted");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOGGER.log(Level.ERROR, "Announce failed", e);
                pause();
            }
        }
    }

    private void scheduleAnnounce(int delaySecs) {
        if (responseScheduledFuture != null && !responseScheduledFuture.isDone()) {
            LOGGER.log(Level.TRACE, "Cancelling scheduled announce request");
            responseScheduledFuture.cancel(true);
        }
        LOGGER.log(Level.DEBUG, "Scheduled next announce request in " + delaySecs + " seconds");
        responseScheduledFuture = EXECUTOR_SERVICE.schedule(announceTask, delaySecs, TimeUnit.SECONDS);
    }

    private void waitForActiveState() {
        lock.lock();
        try {
            while (!isActive) {
                LOGGER.log(Level.TRACE, "Waiting for active state");
                condition.await();
                LOGGER.log(Level.TRACE, "Received active state");
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.ERROR, "Waiting for active state interrupted");
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public void pause() {
        lock.lock();
        try {
            LOGGER.log(Level.TRACE, "Pausing tracker handler");
            isActive = false;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void resume() {
        lock.lock();
        try {
            LOGGER.log(Level.TRACE, "Resuming tracker handler");
            isActive = true;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public interface Listener {

        void onAnnounceResponse(List<PeerResponse> peerResponses);
    }

    private class AnnounceTask implements Callable<AnnounceResponse> {

        private Long connectionId;
        private LocalDateTime connectionIdExpiration;

        @Override
        public AnnounceResponse call() {
            LOGGER.log(Level.TRACE, "Running announce task");

            for (int i = 0; i < MAX_TRIES; i++) {
                int timeout = calculateTimeout(i);
                try {
                    tracker.setTimeout(timeout);
                    return announce(infoHash);
                } catch (IOException e) {
                    // TODO: handle each failure type separately. Assume only will fail due to timeout for now.
                    LOGGER.log(Level.WARNING, "Announce timed out after " + timeout + "ms");
                }
            }

            LOGGER.log(Level.ERROR, "Announce exceeded max tries");
            throw new ExceededMaxTriesException("announce task", MAX_TRIES);
        }

        private int calculateTimeout(int tries) {
            return (int) Math.pow(2, tries) * TIMEOUT_MILLIS;
        }

        private AnnounceResponse announce(Sha1Hash infoHash) throws IOException {
            LOGGER.log(Level.INFO, "Announcing to tracker");

            if (!hasValidConnectionId()) {
                getConnectionId();
            }

            // TODO: populate fields with actual values
            AnnounceRequest announceRequest = new AnnounceRequest(connectionId,
                    infoHash,
                    PEER_ID.getBytes(),
                    0,
                    0,
                    0,
                    Event.NONE,
                    0,
                    0,
                    -1,
                    PORT
            );

            tracker.sendRequest(announceRequest);
            AnnounceResponse announceResponse = tracker.receiveAnnounceResponse();

            if (!announceResponse.hasMatchingTransactionId(announceRequest)) {
                throw new IOException("Transaction ID mismatch");
            }

            LOGGER.log(Level.DEBUG, "Received announce response: {0}", announceResponse);
            return announceResponse;
        }

        private boolean hasValidConnectionId() {
            if (connectionId == null) {
                return false;
            }
            assert connectionIdExpiration != null;
            return LocalDateTime.now().isBefore(connectionIdExpiration);
        }

        private void getConnectionId() throws IOException {
            LOGGER.log(Level.TRACE, "Getting connection ID");
            ConnectionRequest connectionRequest = new ConnectionRequest();
            tracker.sendRequest(connectionRequest);
            ConnectionResponse connectionResponse = tracker.receiveConnectionResponse();
            if (!connectionResponse.hasMatchingTransactionId(connectionRequest)) {
                throw new IOException("Transaction ID mismatch");
            }
            connectionId = connectionResponse.getConnectionId();
            connectionIdExpiration = LocalDateTime.now().plusMinutes(CONNECTION_ID_EXPIRATION_MINS);
            LOGGER.log(Level.DEBUG, "Received connection ID: " + connectionId);
        }
    }
}
