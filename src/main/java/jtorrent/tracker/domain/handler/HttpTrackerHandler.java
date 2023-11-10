package jtorrent.tracker.domain.handler;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.tracker.domain.model.AnnounceResponse;
import jtorrent.tracker.domain.model.Event;
import jtorrent.tracker.domain.model.http.HttpTracker;

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
        public AnnounceResponse call() throws Exception {
            Sha1Hash infoHash = torrentProgressProvider.getInfoHash();
            long downloaded = torrentProgressProvider.getDownloaded();
            long left = torrentProgressProvider.getLeft();
            long uploaded = torrentProgressProvider.getUploaded();
            return tracker.announce(infoHash, downloaded, left, uploaded, event);
        }
    }
}
