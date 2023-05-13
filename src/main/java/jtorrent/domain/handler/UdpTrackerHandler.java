package jtorrent.domain.handler;

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

import jtorrent.domain.handler.exception.ExceededMaxTriesException;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.udp.UdpTracker;
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
    private final Torrent torrent;
    private final List<Listener> listeners = new ArrayList<>();
    private final Thread announceSchedulerThread = new Thread(this);
    private Long connectionId;
    private LocalDateTime connectionIdExpiration;

    public UdpTrackerHandler(UdpTracker tracker, Torrent torrent) {
        this.tracker = tracker;
        this.torrent = torrent;
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

        ScheduledFuture<AnnounceResponse> scheduledFuture = scheduleAnnounce(Event.STARTED, 0);

        while (!announceSchedulerThread.isInterrupted()) {
            try {
                LOGGER.log(Level.TRACE, "Waiting for announce to execute");
                AnnounceResponse announceResponse = scheduledFuture.get();
                LOGGER.log(Level.TRACE, "Received announce response");
                listeners.forEach(listener -> listener.onAnnounceResponse(announceResponse.getPeers()));
                int interval = announceResponse.getInterval();
                scheduledFuture = scheduleAnnounce(Event.NONE, interval);
            } catch (InterruptedException e) {
                LOGGER.log(Level.DEBUG, "Interrupted while waiting for announce to execute");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOGGER.log(Level.ERROR, "Announce failed: {}", e.getMessage());
                return;
            }
        }

        scheduledFuture.cancel(true);
        LOGGER.log(Level.DEBUG, "Announce scheduler thread stopped");
    }

    private ScheduledFuture<AnnounceResponse> scheduleAnnounce(Event event, long delaySecs) {
        LOGGER.log(Level.DEBUG, "Scheduled next announce request ({0}) in {1} seconds", event, delaySecs);
        return EXECUTOR_SERVICE.schedule(new AnnounceTask(event), delaySecs, TimeUnit.SECONDS);
    }

    public void start() {
        LOGGER.log(Level.DEBUG, "Starting tracker handler");
        announceSchedulerThread.start();
    }

    public void stop() {
        LOGGER.log(Level.DEBUG, "Stopping tracker handler");
        announceSchedulerThread.interrupt();
        new AnnounceTask(Event.STOPPED).call();
    }

    public interface Listener {

        void onAnnounceResponse(List<PeerResponse> peerResponses);
    }

    private class AnnounceTask implements Callable<AnnounceResponse> {

        private final Event event;

        public AnnounceTask(Event event) {
            this.event = event;
        }

        @Override
        public AnnounceResponse call() {
            LOGGER.log(Level.TRACE, "Running announce task");
            return tryAnnounce(event);
        }

        private AnnounceResponse tryAnnounce(Event event) {
            for (int i = 0; i < MAX_TRIES; i++) {
                int timeout = calculateTimeout(i);
                try {
                    tracker.setTimeout(timeout);
                    return announce(event);
                } catch (IOException e) {
                    // TODO: handle each failure type separately. Assume only will fail due to timeout for now.
                    LOGGER.log(Level.WARNING, "Announce timed out after " + timeout + "ms");
                }
            }

            throw new ExceededMaxTriesException("announce task", MAX_TRIES);
        }

        private int calculateTimeout(int tries) {
            return (int) Math.pow(2, tries) * TIMEOUT_MILLIS;
        }

        private AnnounceResponse announce(Event event) throws IOException {
            LOGGER.log(Level.INFO, "Announcing to tracker");

            if (!hasValidConnectionId()) {
                getConnectionId();
            }

            // TODO: what to set for ipv4, key, numWant?
            AnnounceRequest announceRequest = new AnnounceRequest(connectionId,
                    torrent.getInfoHash(),
                    PEER_ID.getBytes(),
                    torrent.getDownloaded(),
                    torrent.getLeft(),
                    torrent.getUploaded(),
                    event,
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
