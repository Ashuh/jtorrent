package jtorrent.domain.common.util.logging;

import java.net.InetAddress;
import java.net.URI;

import org.slf4j.MDC;

import jtorrent.domain.peer.model.Peer;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.tracker.model.Tracker;

public class MdcUtil {

    private static final String KEY_TORRENT = "torrent";
    private static final String KEY_PEER = "peer";
    private static final String KEY_TRACKER = "tracker";

    private MdcUtil() {
    }

    public static void putTorrent(Torrent torrent) {
        MDC.put(KEY_TORRENT, torrent.getInfoHash().toString());
    }

    public static void removeTorrent() {
        MDC.remove(KEY_TORRENT);
    }

    public static void putPeer(Peer peer) {
        putPeer(peer.getPeerContactInfo());
    }

    public static void putPeer(InetAddress address, int port) {
        putPeer(new PeerContactInfo(address, port));
    }

    public static void putPeer(PeerContactInfo peerContactInfo) {
        MDC.put(KEY_PEER, peerContactInfo.getAddress().getHostAddress() + "-" + peerContactInfo.getPort());
    }

    public static void removePeer() {
        MDC.remove(KEY_PEER);
    }

    public static void putTracker(Tracker tracker) {
        URI uri = tracker.getUri();
        MDC.put(KEY_TRACKER, uri.getScheme() + "-" + uri.getHost() + "-" + uri.getPort());
    }

    public static void removeTracker() {
        MDC.remove(KEY_TRACKER);
    }
}
