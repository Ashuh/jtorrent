package jtorrent.domain.tracker.handler;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.SocketException;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.tracker.handler.exception.ExceededMaxTriesException;
import jtorrent.domain.tracker.model.Event;
import jtorrent.domain.tracker.model.udp.UdpTracker;
import jtorrent.domain.tracker.model.udp.message.response.UdpAnnounceResponse;

public class UdpTrackerHandler extends TrackerHandler {

    private static final Logger LOGGER = System.getLogger(UdpTrackerHandler.class.getName());
    private static final int TIMEOUT_MILLIS = 15000;
    private static final int MAX_TRIES = 8;

    private final UdpTracker tracker;

    public UdpTrackerHandler(TorrentProgressProvider torrentProgressProvider, UdpTracker tracker) {
        super(torrentProgressProvider);
        this.tracker = requireNonNull(tracker);
        try {
            this.tracker.init();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected AnnounceTask createAnnounceTask(Event event) {
        return new UdpAnnounceTask(event);
    }

    public class UdpAnnounceTask extends AnnounceTask {

        public UdpAnnounceTask(Event event) {
            super(event);
        }

        @Override
        public UdpAnnounceResponse call() {
            LOGGER.log(Level.TRACE, "Running announce task");
            return tryAnnounce(event);
        }

        private UdpAnnounceResponse tryAnnounce(Event event) {
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

        private UdpAnnounceResponse announce(Event event) throws IOException {
            LOGGER.log(Level.INFO, "Announcing to tracker");

            if (!tracker.hasValidConnectionId()) {
                tracker.connect();
            }

            Sha1Hash infoHash = torrentProgressProvider.getInfoHash();
            long downloaded = torrentProgressProvider.getDownloaded();
            long left = torrentProgressProvider.getLeft();
            long uploaded = torrentProgressProvider.getUploaded();
            return tracker.announce(infoHash, downloaded, left, uploaded, event);
        }
    }
}
