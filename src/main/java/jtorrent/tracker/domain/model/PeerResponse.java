package jtorrent.tracker.domain.model;

import java.net.InetAddress;
import java.util.Optional;

import jtorrent.peer.domain.model.PeerContactInfo;

public interface PeerResponse {

    default PeerContactInfo toPeerContactInfo() {
        return new PeerContactInfo(getIp(), getPort());
    }

    InetAddress getIp();

    int getPort();

    Optional<String> getPeerId();
}
