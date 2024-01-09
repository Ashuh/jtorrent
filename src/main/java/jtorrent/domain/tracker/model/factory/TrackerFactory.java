package jtorrent.domain.tracker.model.factory;

import java.net.InetSocketAddress;
import java.net.URI;

import jtorrent.domain.tracker.model.Tracker;
import jtorrent.domain.tracker.model.http.HttpTracker;
import jtorrent.domain.tracker.model.udp.UdpTracker;

public class TrackerFactory {

    private TrackerFactory() {
    }

    public static Tracker fromUri(URI uri) {
        String protocol = uri.getScheme();

        switch (protocol) {
        case "https":
        case "http":
            return new HttpTracker(uri);
        case "udp":
            InetSocketAddress address = new InetSocketAddress(uri.getHost(), uri.getPort());
            return new UdpTracker(address);
        default:
            throw new IllegalArgumentException("Unsupported protocol " + protocol);
        }
    }
}
