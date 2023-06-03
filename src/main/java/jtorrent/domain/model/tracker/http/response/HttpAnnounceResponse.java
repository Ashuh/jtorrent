package jtorrent.domain.model.tracker.http.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jtorrent.domain.model.tracker.AnnounceResponse;
import jtorrent.domain.model.tracker.PeerResponse;

public class HttpAnnounceResponse implements AnnounceResponse {

    private static final String KEY_COMPLETE = "complete";
    private static final String KEY_INCOMPLETE = "incomplete";
    private static final String KEY_INTERVAL = "interval";
    private static final String KEY_PEERS = "peers";

    private final int interval;
    private final int complete;
    private final int incomplete;
    private final List<HttpPeerResponse> peers;

    public HttpAnnounceResponse(int interval, int complete, int incomplete, List<HttpPeerResponse> peers) {
        this.interval = interval;
        this.complete = complete;
        this.incomplete = incomplete;
        this.peers = peers;
    }

    public static HttpAnnounceResponse fromMap(Map<String, Object> map) {
        int interval = ((Long) map.get(KEY_INTERVAL)).intValue();
        int complete = ((Long) map.get(KEY_COMPLETE)).intValue();
        int incomplete = ((Long) map.get(KEY_INCOMPLETE)).intValue();
        List<HttpPeerResponse> peers = ((List<Map<String, Object>>) map.get(KEY_PEERS)).stream()
                .map(HttpPeerResponse::fromMap)
                .collect(Collectors.toList());
        return new HttpAnnounceResponse(interval, complete, incomplete, peers);
    }

    public int getInterval() {
        return interval;
    }

    @Override
    public int getLeechers() {
        return incomplete;
    }

    @Override
    public int getSeeders() {
        return complete;
    }

    @Override
    public List<PeerResponse> getPeers() {
        return new ArrayList<>(peers);
    }

    @Override
    public String toString() {
        return "HttpAnnounceResponse{"
                + "interval=" + interval
                + ", complete=" + complete
                + ", incomplete=" + incomplete
                + ", peers=" + peers
                + '}';
    }
}
