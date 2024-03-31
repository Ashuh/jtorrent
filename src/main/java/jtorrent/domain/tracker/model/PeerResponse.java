package jtorrent.domain.tracker.model;

import java.net.InetAddress;
import java.util.Optional;

import jtorrent.domain.peer.model.PeerContactInfo;

public interface PeerResponse {

    default PeerContactInfo toPeerContactInfo() {
        return new PeerContactInfo(getIp(), getPort());
    }

    InetAddress getIp();

    int getPort();

    Optional<String> getPeerId();
}
