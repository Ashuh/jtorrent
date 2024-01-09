package jtorrent.domain.tracker.handler.factory;

import jtorrent.domain.tracker.handler.HttpTrackerHandler;
import jtorrent.domain.tracker.handler.TrackerHandler;
import jtorrent.domain.tracker.handler.UdpTrackerHandler;
import jtorrent.domain.tracker.model.Tracker;
import jtorrent.domain.tracker.model.http.HttpTracker;
import jtorrent.domain.tracker.model.udp.UdpTracker;

public class TrackerHandlerFactory {

    private TrackerHandlerFactory() {
    }

    public static TrackerHandler create(TrackerHandler.TorrentProgressProvider torrentProgressProvider,
            Tracker tracker) {
        if (tracker instanceof HttpTracker) {
            return new HttpTrackerHandler(torrentProgressProvider, (HttpTracker) tracker);
        } else if (tracker instanceof UdpTracker) {
            return new UdpTrackerHandler(torrentProgressProvider, (UdpTracker) tracker);
        }
        throw new IllegalArgumentException("Unsupported tracker type: " + tracker.getClass().getName());
    }
}
