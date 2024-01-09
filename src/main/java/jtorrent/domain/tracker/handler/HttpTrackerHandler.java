package jtorrent.domain.tracker.handler;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.tracker.model.AnnounceResponse;
import jtorrent.domain.tracker.model.Event;
import jtorrent.domain.tracker.model.http.HttpTracker;

public class HttpTrackerHandler extends TrackerHandler {

    private static final System.Logger LOGGER = System.getLogger(HttpTrackerHandler.class.getName());

    private final HttpTracker tracker;

    public HttpTrackerHandler(TorrentProgressProvider torrentProgressProvider, HttpTracker tracker) {
        super(torrentProgressProvider);
        this.tracker = requireNonNull(tracker);
    }

    @Override
    protected AnnounceTask createAnnounceTask(Event event) {
        return new HttpAnnounceTask(event);
    }

    private class HttpAnnounceTask extends AnnounceTask {

        public HttpAnnounceTask(Event event) {
            super(event);
        }

        @Override
        public AnnounceResponse call() throws IOException {
            Sha1Hash infoHash = torrentProgressProvider.getInfoHash();
            long downloaded = torrentProgressProvider.getDownloaded();
            long left = torrentProgressProvider.getLeft();
            long uploaded = torrentProgressProvider.getUploaded();
            return tracker.announce(infoHash, downloaded, left, uploaded, event);
        }
    }
}
