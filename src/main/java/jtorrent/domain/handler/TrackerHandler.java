package jtorrent.domain.handler;

import static java.util.Objects.requireNonNull;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.AnnounceResponse;
import jtorrent.domain.model.tracker.Event;
import jtorrent.domain.model.tracker.PeerResponse;
import jtorrent.domain.model.tracker.Tracker;
import jtorrent.domain.model.tracker.http.HttpTracker;
import jtorrent.domain.model.tracker.udp.UdpTracker;

public abstract class TrackerHandler implements Runnable {

    private static final Logger LOGGER = System.getLogger(TrackerHandler.class.getName());
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

    protected final Torrent torrent;
    private final List<Listener> listeners = new ArrayList<>();
    private final Thread announceSchedulerThread = new Thread(this);

    protected TrackerHandler(Torrent torrent) {
        this.torrent = requireNonNull(torrent);
    }

    public static TrackerHandler create(Torrent torrent, Tracker tracker) {
        if (tracker instanceof HttpTracker) {
            return new HttpTrackerHandler(torrent, (HttpTracker) tracker);
        } else if (tracker instanceof UdpTracker) {
            return new UdpTrackerHandler(torrent, (UdpTracker) tracker);
        }
        throw new IllegalArgumentException("Unsupported tracker type: " + tracker.getClass().getName());
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void start() {
        LOGGER.log(Logger.Level.DEBUG, "Starting tracker handler");
        announceSchedulerThread.start();
    }

    public void stop() {
        LOGGER.log(Logger.Level.DEBUG, "Stopping tracker handler");
        announceSchedulerThread.interrupt();
        try {
            createAnnounceTask(Event.STOPPED).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        ScheduledFuture<AnnounceResponse> scheduledFuture = scheduleAnnounce(Event.STARTED, 0);

        while (!announceSchedulerThread.isInterrupted()) {
            try {
                LOGGER.log(Logger.Level.TRACE, "Waiting for announce to execute");
                AnnounceResponse announceResponse = scheduledFuture.get();
                LOGGER.log(Logger.Level.TRACE, "Received announce response {0}", announceResponse);
                List<PeerResponse> peerResponses = new ArrayList<>(announceResponse.getPeers());
                listeners.forEach(listener -> listener.onAnnounceResponse(peerResponses));
                int interval = announceResponse.getInterval();
                scheduledFuture = scheduleAnnounce(Event.NONE, interval);
            } catch (InterruptedException e) {
                LOGGER.log(Logger.Level.DEBUG, "Interrupted while waiting for announce to execute");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOGGER.log(Logger.Level.ERROR, "Announce failed: {0}", e.getMessage());
                return;
            }
        }

        scheduledFuture.cancel(true);
        LOGGER.log(Logger.Level.DEBUG, "Announce scheduler thread stopped");
    }

    protected ScheduledFuture<AnnounceResponse> scheduleAnnounce(Event event, long delaySecs) {
        LOGGER.log(Logger.Level.DEBUG, "Scheduled next announce request ({0}) in {1} seconds", event, delaySecs);
        return EXECUTOR_SERVICE.schedule(createAnnounceTask(event), delaySecs, TimeUnit.SECONDS);
    }

    protected abstract AnnounceTask createAnnounceTask(Event event);

    protected abstract static class AnnounceTask implements Callable<AnnounceResponse> {

        protected final Event event;

        protected AnnounceTask(Event event) {
            this.event = requireNonNull(event);
        }
    }

    public interface Listener {

        void onAnnounceResponse(List<PeerResponse> peerResponses);
    }
}
