package jtorrent.domain.handler;

import static java.util.Objects.requireNonNull;

import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.AnnounceResponse;
import jtorrent.domain.model.tracker.Event;
import jtorrent.domain.model.tracker.http.HttpTracker;

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
            return tracker.announce(torrent, event);
        }
    }
}
