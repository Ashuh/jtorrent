package jtorrent.domain.model.tracker.udp.message;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Objects;

import jtorrent.domain.model.exception.UnpackException;
import jtorrent.domain.model.peer.Peer;

public class PeerResponse {

    public static final int BYTES = 6;

    private final int ipv4;
    private final int port; // unsigned short

    public PeerResponse(int ipv4, int port) {
        this.ipv4 = ipv4;
        this.port = port;
    }

    public static PeerResponse unpack(byte[] payload) {
        if (payload.length != BYTES) {
            throw new UnpackException("Expected " + BYTES + " bytes but got " + payload.length + " bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int ipv4 = buffer.getInt();
        int port = buffer.getShort();
        return new PeerResponse(ipv4, port);
    }

    public Peer toPeer() {
        byte[] ipv4Bytes = ByteBuffer.allocate(Integer.BYTES)
                .putInt(ipv4)
                .array();
        InetAddress address = null;
        try {
            address = InetAddress.getByAddress(ipv4Bytes);
        } catch (UnknownHostException ignored) {
            // this should never happen
        }

        return new Peer(address, port);
    }

    public int getIpv4() {
        return ipv4;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipv4, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PeerResponse that = (PeerResponse) o;
        return ipv4 == that.ipv4 && port == that.port;
    }

    @Override
    public String toString() {
        return "PeerResponse{"
                + "ipv4=" + ipv4
                + ", port=" + port
                + '}';
    }
}
