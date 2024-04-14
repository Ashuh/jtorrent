package jtorrent.domain.common.util.logging;

import java.net.InetAddress;

import org.slf4j.MDC;

import jtorrent.domain.peer.model.Peer;
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
}
