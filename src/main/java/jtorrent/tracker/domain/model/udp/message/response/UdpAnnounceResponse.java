package jtorrent.tracker.domain.model.udp.message.response;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jtorrent.common.domain.exception.UnpackException;
import jtorrent.tracker.domain.model.AnnounceResponse;
import jtorrent.tracker.domain.model.PeerResponse;
import jtorrent.tracker.domain.model.udp.message.UdpMessage;

/**
 * Represents an announce response.
 *
 * @see <a href="https://www.bittorrent.org/beps/bep_0015.html">UDP Tracker Protocol for BitTorrent</a>
 */
public class UdpAnnounceResponse extends UdpMessage implements AnnounceResponse {

    public static final int MESSAGE_MIN_BYTES = 20;
    public static final int PAYLOAD_MIN_BYTES = 16;

    private final int interval;
    private final int leechers;
    private final int seeders;
    private final List<UdpPeerResponse> peers;

    public UdpAnnounceResponse(int transactionId, int interval, int leechers, int seeders,
            List<UdpPeerResponse> peers) {
        super(transactionId);
        this.interval = interval;
        this.leechers = leechers;
        this.seeders = seeders;
        this.peers = requireNonNull(peers);
    }

    public static UdpAnnounceResponse unpack(byte[] payload) {
        if (payload.length < PAYLOAD_MIN_BYTES) {
            throw new UnpackException(
                    "Expected at least " + PAYLOAD_MIN_BYTES + " bytes but got " + payload.length + " bytes.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int transactionId = buffer.getInt();
        int interval = buffer.getInt();
        int leechers = buffer.getInt();
        int seeders = buffer.getInt();
        List<UdpPeerResponse> peers = new ArrayList<>();

        while (buffer.hasRemaining()) {
            byte[] peerBytes = new byte[UdpPeerResponse.BYTES];
            buffer.get(peerBytes);
            UdpPeerResponse udpPeerResponse = UdpPeerResponse.unpack(peerBytes);
            peers.add(udpPeerResponse);
        }

        return new UdpAnnounceResponse(transactionId, interval, leechers, seeders, peers);
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
        return new ArrayList<>(peers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval, leechers, seeders, peers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UdpAnnounceResponse that = (UdpAnnounceResponse) o;
        return interval == that.interval
                && leechers == that.leechers
                && seeders == that.seeders
                && peers.equals(that.peers);
    }

    @Override
    public String toString() {
        return "UdpAnnounceResponse{"
                + "interval=" + interval
                + ", leechers=" + leechers
                + ", seeders=" + seeders
                + ", peers=" + peers
                + ", transactionId=" + transactionId
                + '}';
    }
}
