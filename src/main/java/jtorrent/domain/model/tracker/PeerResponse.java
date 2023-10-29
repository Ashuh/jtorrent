package jtorrent.domain.model.tracker;

import java.net.InetAddress;
import java.util.Optional;

import jtorrent.domain.model.peer.PeerContactInfo;

public interface PeerResponse {

    default PeerContactInfo toPeerContactInfo() {
        return new PeerContactInfo(getIp(), getPort());
    }

    InetAddress getIp();

    int getPort();

    Optional<String> getPeerId();
}
