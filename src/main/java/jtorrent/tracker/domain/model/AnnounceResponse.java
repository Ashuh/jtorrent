package jtorrent.tracker.domain.model;

import java.util.List;

public interface AnnounceResponse {

    int getInterval();

    int getLeechers();

    int getSeeders();

    List<PeerResponse> getPeers();
}
