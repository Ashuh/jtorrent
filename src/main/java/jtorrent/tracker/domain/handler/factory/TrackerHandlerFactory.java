package jtorrent.tracker.domain.handler.factory;

import jtorrent.tracker.domain.model.Tracker;
import jtorrent.tracker.domain.model.http.HttpTracker;
import jtorrent.tracker.domain.model.udp.UdpTracker;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.tracker.domain.handler.HttpTrackerHandler;
import jtorrent.tracker.domain.handler.TrackerHandler;
import jtorrent.tracker.domain.handler.UdpTrackerHandler;

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
