package jtorrent.domain.model.tracker;

import java.net.InetAddress;
import java.util.Optional;

import jtorrent.domain.model.peer.OutgoingPeer;
import jtorrent.domain.model.peer.Peer;

public interface PeerResponse {

    default Peer toPeer() {
        return new OutgoingPeer(getIp(), getPort());
    }

    InetAddress getIp();

    int getPort();

    Optional<String> getPeerId();
}
