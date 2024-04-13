package jtorrent.domain.common.util.logging;

import java.net.InetAddress;

import org.slf4j.MDC;

import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.torrent.model.Torrent;

public class MdcUtil {

    private static final String KEY_TORRENT = "torrent";
    private static final String KEY_PEER = "peer";

    private MdcUtil() {
    }

    public static void putTorrent(Torrent torrent) {
        MDC.put(KEY_TORRENT, torrent.getInfoHash().toString());
    }

    public static void removeTorrent() {
        MDC.remove(KEY_TORRENT);
    }

    public static void putPeerContactInfo(InetAddress address, int port) {
        putPeerContactInfo(new PeerContactInfo(address, port));
    }

    public static void putPeerContactInfo(PeerContactInfo peerContactInfo) {
        MDC.put(KEY_PEER, peerContactInfo.toString());
    }

    public static void removePeerContactInfo() {
        MDC.remove(KEY_PEER);
    }
}
