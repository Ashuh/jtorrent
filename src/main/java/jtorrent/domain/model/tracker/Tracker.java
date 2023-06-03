package jtorrent.domain.model.tracker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.http.HttpTracker;
import jtorrent.domain.model.tracker.udp.UdpTracker;

public abstract class Tracker {

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

    public abstract AnnounceResponse announce(Torrent torrent, Event event) throws IOException;
}
