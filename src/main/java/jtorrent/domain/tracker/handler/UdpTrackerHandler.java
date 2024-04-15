package jtorrent.domain.tracker.handler;

import java.io.IOException;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.tracker.handler.exception.ExceededMaxTriesException;
import jtorrent.domain.tracker.model.Event;
import jtorrent.domain.tracker.model.udp.UdpTracker;
import jtorrent.domain.tracker.model.udp.message.response.UdpAnnounceResponse;

public class UdpTrackerHandler extends TrackerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UdpTrackerHandler.class);
    private static final int TIMEOUT_MILLIS = 15000;
    private static final int MAX_TRIES = 8;

    public UdpTrackerHandler(TorrentProgressProvider torrentProgressProvider, UdpTracker tracker) {
        super(tracker, torrentProgressProvider);
        try {
            getTracker().init();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UdpTracker getTracker() {
        return (UdpTracker) super.getTracker();
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
            return tryAnnounce(event);
        }

        private UdpAnnounceResponse tryAnnounce(Event event) {
            for (int i = 0; i < MAX_TRIES; i++) {
                int timeout = calculateTimeout(i);
                try {
                    getTracker().setTimeout(timeout);
                    return announce(event);
                } catch (IOException e) {
                    // TODO: handle each failure type separately. Assume only will fail due to timeout for now.
                    LOGGER.warn(Markers.TRACKER, "Failed to announce: timeout after {}ms", timeout);
                }
            }

            throw new ExceededMaxTriesException("announce task", MAX_TRIES);
        }

        private int calculateTimeout(int tries) {
            return (int) Math.pow(2, tries) * TIMEOUT_MILLIS;
        }

        private UdpAnnounceResponse announce(Event event) throws IOException {
            LOGGER.debug(Markers.TRACKER, "Announcing to tracker");

            if (!getTracker().hasValidConnectionId()) {
                getTracker().connect();
            }

            Sha1Hash infoHash = torrentProgressProvider.getInfoHash();
            long downloaded = torrentProgressProvider.getDownloaded();
            long left = torrentProgressProvider.getLeft();
            long uploaded = torrentProgressProvider.getUploaded();
            UdpAnnounceResponse response = getTracker().announce(infoHash, downloaded, left, uploaded, event);
            LOGGER.info(Markers.TRACKER, "Announce completed successfully");
            return response;
        }
    }
}
