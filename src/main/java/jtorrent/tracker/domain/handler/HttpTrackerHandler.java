package jtorrent.tracker.domain.handler;

import static java.util.Objects.requireNonNull;

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.tracker.domain.model.AnnounceResponse;
import jtorrent.tracker.domain.model.Event;
import jtorrent.tracker.domain.model.http.HttpTracker;

public class HttpTrackerHandler extends TrackerHandler {

    private static final System.Logger LOGGER = System.getLogger(HttpTrackerHandler.class.getName());

    private final HttpTracker tracker;

    public HttpTrackerHandler(Torrent torrent, HttpTracker tracker) {
        super(torrent);
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
            Sha1Hash infoHash = torrent.getInfoHash();
            long downloaded = torrent.getDownloaded();
            long left = torrent.getRemainingBytes();
            long uploaded = torrent.getUploaded();
            return tracker.announce(infoHash, downloaded, left, uploaded, event);
        }
    }
}
