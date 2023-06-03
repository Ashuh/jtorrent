package jtorrent.domain.model.tracker;

import java.util.List;

public interface AnnounceResponse {

    int getInterval();

    int getLeechers();

    int getSeeders();

    List<PeerResponse> getPeers();
}
