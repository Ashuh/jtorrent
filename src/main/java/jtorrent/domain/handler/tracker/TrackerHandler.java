package jtorrent.domain.handler.tracker;

import static java.util.Objects.requireNonNull;

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

import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.AnnounceResponse;
import jtorrent.domain.model.tracker.Event;
import jtorrent.domain.model.tracker.PeerResponse;
import jtorrent.domain.util.BackgroundTask;

public abstract class TrackerHandler {

    private static final Logger LOGGER = System.getLogger(TrackerHandler.class.getName());

    protected final Torrent torrent;
    private final List<Listener> listeners = new ArrayList<>();
    private final PeriodicAnnounceTask periodicAnnounceTask = new PeriodicAnnounceTask();

    protected TrackerHandler(Torrent torrent) {
        this.torrent = requireNonNull(torrent);
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
    }

    protected abstract AnnounceTask createAnnounceTask(Event event);

    public interface Listener {

        void onAnnounceResponse(List<PeerResponse> peerResponses);
    }

    private class PeriodicAnnounceTask extends BackgroundTask {

        private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        private ScheduledFuture<AnnounceResponse> scheduledFuture;

        @Override
        protected void doOnStarted() {
            scheduleAnnounce(Event.STARTED, 0);
        }

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
        protected void doOnStop() {
            cancelAnnounce();
            scheduleAnnounce(Event.STOPPED, 0);
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
    }

    protected abstract static class AnnounceTask implements Callable<AnnounceResponse> {

        protected final Event event;

        protected AnnounceTask(Event event) {
            this.event = requireNonNull(event);
        }
    }
}
