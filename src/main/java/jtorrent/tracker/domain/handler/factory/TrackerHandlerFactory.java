package jtorrent.tracker.domain.handler.factory;

import jtorrent.tracker.domain.handler.HttpTrackerHandler;
import jtorrent.tracker.domain.handler.TrackerHandler;
import jtorrent.tracker.domain.handler.UdpTrackerHandler;
import jtorrent.tracker.domain.model.Tracker;
import jtorrent.tracker.domain.model.http.HttpTracker;
import jtorrent.tracker.domain.model.udp.UdpTracker;

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
