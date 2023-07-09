package jtorrent.domain.handler.tracker.factory;

import jtorrent.domain.handler.tracker.HttpTrackerHandler;
import jtorrent.domain.handler.tracker.TrackerHandler;
import jtorrent.domain.handler.tracker.UdpTrackerHandler;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.Tracker;
import jtorrent.domain.model.tracker.http.HttpTracker;
import jtorrent.domain.model.tracker.udp.UdpTracker;

public class TrackerHandlerFactory {

    private TrackerHandlerFactory() {
    }

    public static TrackerHandler create(Torrent torrent, Tracker tracker) {
        if (tracker instanceof HttpTracker) {
            return new HttpTrackerHandler(torrent, (HttpTracker) tracker);
        } else if (tracker instanceof UdpTracker) {
            return new UdpTrackerHandler(torrent, (UdpTracker) tracker);
        }
        throw new IllegalArgumentException("Unsupported tracker type: " + tracker.getClass().getName());
    }
}
