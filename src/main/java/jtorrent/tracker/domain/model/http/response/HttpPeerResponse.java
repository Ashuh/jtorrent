package jtorrent.tracker.domain.model.http.response;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;

import jtorrent.tracker.domain.model.PeerResponse;

public class HttpPeerResponse implements PeerResponse {

    private static final String KEY_IP = "ip";
    private static final String KEY_PORT = "port";
    private static final String KEY_PEER_ID = "peer id";

    private final String ip;
    private final int port;
    private final String peerId;

    public HttpPeerResponse(String ip, int port, String peerId) {
        this.ip = ip;
        this.port = port;
        this.peerId = requireNonNull(peerId);
    }

    public static HttpPeerResponse fromMap(Map<String, Object> map) {
        String ip = (String) map.get(KEY_IP);
        int port = ((Long) map.get(KEY_PORT)).intValue();
        String peerId = (String) map.get(KEY_PEER_ID);
        return new HttpPeerResponse(ip, port, peerId);
    }

    @Override
    public InetAddress getIp() {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException ignored) {
            // this should never happen
        }
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Optional<String> getPeerId() {
        return Optional.of(peerId);
    }

    @Override
    public String toString() {
        return "HttpPeerResponse{"
                + "ip=" + ip
                + ", port=" + port
                + ", peerId='" + peerId + '\''
                + '}';
    }
}
