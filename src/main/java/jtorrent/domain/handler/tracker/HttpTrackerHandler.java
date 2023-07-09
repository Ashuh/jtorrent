package jtorrent.domain.handler.tracker;

import static java.util.Objects.requireNonNull;

import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.AnnounceResponse;
import jtorrent.domain.model.tracker.Event;
import jtorrent.domain.model.tracker.http.HttpTracker;
import jtorrent.domain.util.Sha1Hash;

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
