package jtorrent.domain.tracker.handler;

import java.io.IOException;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.tracker.model.Event;
import jtorrent.domain.tracker.model.http.HttpTracker;
import jtorrent.domain.tracker.model.http.response.HttpAnnounceResponse;

public class HttpTrackerHandler extends TrackerHandler {

    public HttpTrackerHandler(TorrentProgressProvider torrentProgressProvider, HttpTracker tracker) {
        super(tracker, torrentProgressProvider);
    }

    @Override
    protected AnnounceTask createAnnounceTask(Event event) {
        return new HttpAnnounceTask(event);
    }

    @Override
    public HttpTracker getTracker() {
        return (HttpTracker) super.getTracker();
    }

    private class HttpAnnounceTask extends AnnounceTask {

        public HttpAnnounceTask(Event event) {
            super(event);
        }

        @Override
        public HttpAnnounceResponse call() throws IOException {
            Sha1Hash infoHash = torrentProgressProvider.getInfoHash();
            long downloaded = torrentProgressProvider.getDownloaded();
            long left = torrentProgressProvider.getLeft();
            long uploaded = torrentProgressProvider.getUploaded();
            return getTracker().announce(infoHash, downloaded, left, uploaded, event);
        }
    }
}
