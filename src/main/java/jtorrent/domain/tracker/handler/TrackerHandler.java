package jtorrent.domain.tracker.handler;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jtorrent.domain.common.util.BackgroundTask;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.common.util.logging.MdcUtil;
import jtorrent.domain.tracker.model.AnnounceResponse;
import jtorrent.domain.tracker.model.Event;
import jtorrent.domain.tracker.model.PeerResponse;
import jtorrent.domain.tracker.model.Tracker;

public abstract class TrackerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerHandler.class);

    protected final TorrentProgressProvider torrentProgressProvider;
    private final Tracker tracker;
    private final List<Listener> listeners = new ArrayList<>();
    private final PeriodicAnnounceTask periodicAnnounceTask = new PeriodicAnnounceTask();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    protected TrackerHandler(Tracker tracker, TorrentProgressProvider torrentProgressProvider) {
        this.tracker = requireNonNull(tracker);
        this.torrentProgressProvider = requireNonNull(torrentProgressProvider);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void start() {
        MdcUtil.putTracker(tracker);
        LOGGER.debug(Markers.TRACKER, "Starting tracker handler");
        periodicAnnounceTask.start();
        LOGGER.info(Markers.TRACKER, "Tracker handler started");
        MdcUtil.removeTracker();
    }

    public void stop() {
        MdcUtil.putTracker(tracker);
        LOGGER.debug(Markers.TRACKER, "Stopping tracker handler");
        periodicAnnounceTask.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOGGER.info(Markers.TRACKER, "Tracker handler stopped");
        MdcUtil.removeTracker();
    }

    public void announceCompleted() {
        MdcUtil.putTracker(tracker);
        AnnounceTask announceTask = createAnnounceTask(Event.COMPLETED);
        Map<String, String> context = MDC.getCopyOfContextMap();
        executorService.submit(() -> {
            MDC.setContextMap(context);
            AnnounceResponse response = announceTask.call();
            MDC.clear();
            return response;
        });
        MdcUtil.removeTracker();
    }

    public Tracker getTracker() {
        return tracker;
    }

    protected abstract AnnounceTask createAnnounceTask(Event event);

    public interface Listener {

        void onAnnounceResponse(List<PeerResponse> peerResponses);
    }

    public interface TorrentProgressProvider {

        /**
         * Gets the info hash of the torrent.
         */
        Sha1Hash getInfoHash();

        /**
         * Gets the number of bytes downloaded.
         */
        long getDownloaded();

        /**
         * Gets the number of bytes left to download.
         */
        long getLeft();

        /**
         * Gets the number of bytes uploaded.
         */
        long getUploaded();
    }

    protected abstract static class AnnounceTask implements Callable<AnnounceResponse> {

        protected final Event event;

        protected AnnounceTask(Event event) {
            this.event = requireNonNull(event);
        }

        @Override
        public abstract AnnounceResponse call() throws IOException;
    }

    private class PeriodicAnnounceTask extends BackgroundTask {

        private ScheduledFuture<AnnounceResponse> scheduledFuture;

        @Override
        protected void execute() throws InterruptedException {
            try {
                AnnounceResponse announceResponse = waitForAnnounceResponse();
                handleAnnounceResponse(announceResponse);
                if (!isStopping()) {
                    scheduleAnnounce(Event.NONE, announceResponse.getInterval());
                }
            } catch (ExecutionException e) {
                LOGGER.error(Markers.TRACKER, "Announce failed", e);
                TrackerHandler.this.stop();
            } catch (CancellationException e) {
                LOGGER.debug(Markers.TRACKER, "Announce cancelled");
            }
        }

        @Override
        protected void doOnStarted() {
            MdcUtil.putTracker(tracker);
            scheduleAnnounce(Event.STARTED, 0);
        }

        @Override
        protected void doOnStop() {
            cancelAnnounce();
            scheduleAnnounce(Event.STOPPED, 0);
            MdcUtil.removeTracker();
        }

        /**
         * Cancels the current announce task.
         * <p>
         * Note: This method should only be called when the task is being stopped, i.e., {@link #doOnStop()}.
         * Otherwise, the thread will continuously try to get the result of the cancelled task.
         */
        private void cancelAnnounce() {
            LOGGER.debug(Markers.TRACKER, "Cancelling announce");
            scheduledFuture.cancel(true);
        }

        private void scheduleAnnounce(Event event, long delaySecs) {
            Map<String, String> context = MDC.getCopyOfContextMap();
            scheduledFuture = executorService.schedule(() -> {
                MDC.setContextMap(context);
                AnnounceResponse response = createAnnounceTask(event).call();
                MDC.clear();
                return response;
            }, delaySecs, TimeUnit.SECONDS);
            LOGGER.debug(Markers.TRACKER, "Scheduled next {} announce in {} seconds", event, delaySecs);
        }

        private AnnounceResponse waitForAnnounceResponse() throws InterruptedException, ExecutionException {
            LOGGER.debug(Markers.TRACKER, "Waiting for announce response");
            return scheduledFuture.get();
        }

        private void handleAnnounceResponse(AnnounceResponse announceResponse) {
            LOGGER.debug(Markers.TRACKER, "Received announce response: {}", announceResponse);
            List<PeerResponse> peerResponses = announceResponse.getPeers();
            listeners.forEach(listener -> listener.onAnnounceResponse(peerResponses));
        }
    }
}
