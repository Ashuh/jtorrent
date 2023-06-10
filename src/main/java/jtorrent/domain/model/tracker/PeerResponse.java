package jtorrent.domain.model.tracker;

import java.net.InetAddress;
import java.util.Optional;

import jtorrent.domain.model.peer.Peer;

public interface PeerResponse {

    default Peer toPeer() {
        InetAddress address = getIp();
        return new Peer(address, getPort());
    }

    InetAddress getIp();

    int getPort();

    Optional<String> getPeerId();
}
