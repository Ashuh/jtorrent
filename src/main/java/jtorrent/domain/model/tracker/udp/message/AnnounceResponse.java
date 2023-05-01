package jtorrent.domain.model.tracker.udp.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an announce response.
 *
 * @see <a href="https://www.bittorrent.org/beps/bep_0015.html">UDP Tracker Protocol for BitTorrent</a>
 */
public class AnnounceResponse extends UdpMessage {

    public static final int MESSAGE_MIN_BYTES = 20;
    public static final int PAYLOAD_MIN_BYTES = 16;

    private final int interval;
    private final int leechers;
    private final int seeders;
    private final List<PeerResponse> peers;

    public AnnounceResponse(int transactionId, int interval, int leechers, int seeders, List<PeerResponse> peers) {
        super(transactionId);
        this.interval = interval;
        this.leechers = leechers;
        this.seeders = seeders;
        this.peers = peers;
    }

    public static AnnounceResponse unpack(byte[] payload) {
        if (payload.length < PAYLOAD_MIN_BYTES) {
            throw new IllegalArgumentException();
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int transactionId = buffer.getInt();
        int interval = buffer.getInt();
        int leechers = buffer.getInt();
        int seeders = buffer.getInt();
        List<PeerResponse> peers = new ArrayList<>();

        while (buffer.hasRemaining()) {
            byte[] peerBytes = new byte[PeerResponse.BYTES];
            buffer.get(peerBytes);
            PeerResponse peerResponse = PeerResponse.unpack(peerBytes);
            peers.add(peerResponse);
        }

        return new AnnounceResponse(transactionId, interval, leechers, seeders, peers);
    }

    public int getInterval() {
        return interval;
    }

    public int getLeechers() {
        return leechers;
    }

    public int getSeeders() {
        return seeders;
    }

    public List<PeerResponse> getPeers() {
        return peers;
    }

    @Override
    public String toString() {
        return "AnnounceResponse{" +
                "interval=" + interval +
                ", leechers=" + leechers +
                ", seeders=" + seeders +
                ", peers=" + peers +
                ", transactionId=" + transactionId +
                '}';
    }
}
