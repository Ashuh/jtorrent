package jtorrent.tracker.domain.handler;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jtorrent.common.domain.util.BackgroundTask;
import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.tracker.domain.model.AnnounceResponse;
import jtorrent.tracker.domain.model.Event;
import jtorrent.tracker.domain.model.PeerResponse;

public abstract class TrackerHandler {

    private static final Logger LOGGER = System.getLogger(TrackerHandler.class.getName());

    protected final TorrentProgressProvider torrentProgressProvider;
    private final List<Listener> listeners = new ArrayList<>();
    private final PeriodicAnnounceTask periodicAnnounceTask = new PeriodicAnnounceTask();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    protected TrackerHandler(TorrentProgressProvider torrentProgressProvider) {
        this.torrentProgressProvider = requireNonNull(torrentProgressProvider);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void start() {
        LOGGER.log(Level.DEBUG, "Starting tracker handler");
        periodicAnnounceTask.start();
    }

    public void stop() {
        LOGGER.log(Level.DEBUG, "Stopping tracker handler");
        periodicAnnounceTask.stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void announceCompleted() {
        AnnounceTask announceTask = createAnnounceTask(Event.COMPLETED);
        executorService.submit(announceTask);
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
                LOGGER.log(Level.ERROR, "Announce failed: {0}", e.getMessage());
                TrackerHandler.this.stop();
            } catch (CancellationException e) {
                LOGGER.log(Level.DEBUG, "Announce cancelled");
            }
        }

        @Override
        protected void doOnStarted() {
            scheduleAnnounce(Event.STARTED, 0);
        }

        @Override
        protected void doOnStop() {
            cancelAnnounce();
            scheduleAnnounce(Event.STOPPED, 0);
        }

        /**
         * Cancels the current announce task.
         * <p>
         * Note: This method should only be called when the task is being stopped, i.e., {@link #doOnStop()}.
         * Otherwise, the thread will continuously try to get the result of the cancelled task.
         */
        private void cancelAnnounce() {
            LOGGER.log(Level.DEBUG, "Cancelling announce");
            scheduledFuture.cancel(true);
        }

        private void scheduleAnnounce(Event event, long delaySecs) {
            scheduledFuture = executorService.schedule(createAnnounceTask(event), delaySecs, TimeUnit.SECONDS);
            LOGGER.log(Level.DEBUG, "Scheduled next announce ({0}) in {1} seconds", event, delaySecs);
        }

        private AnnounceResponse waitForAnnounceResponse() throws InterruptedException, ExecutionException {
            LOGGER.log(Level.DEBUG, "Waiting for announce response");
            return scheduledFuture.get();
        }

        private void handleAnnounceResponse(AnnounceResponse announceResponse) {
            LOGGER.log(Level.DEBUG, "Handling announce response {0}", announceResponse);
            List<PeerResponse> peerResponses = announceResponse.getPeers();
            listeners.forEach(listener -> listener.onAnnounceResponse(peerResponses));
        }
    }
}
