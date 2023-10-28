package jtorrent.domain.model.tracker;

import java.net.InetAddress;
import java.util.Optional;

import jtorrent.domain.model.peer.OutgoingPeer;
import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.model.peer.PeerContactInfo;

public interface PeerResponse {

    default Peer toPeer() {
        return new OutgoingPeer(new PeerContactInfo(getIp(), getPort()));
    }

    InetAddress getIp();

    int getPort();

    Optional<String> getPeerId();
}
