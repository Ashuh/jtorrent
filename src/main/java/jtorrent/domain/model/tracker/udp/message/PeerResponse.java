package jtorrent.domain.model.tracker.udp.message;

import java.nio.ByteBuffer;

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
            throw new IllegalArgumentException();
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int ipv4 = buffer.getInt();
        int port = buffer.getShort();
        return new PeerResponse(ipv4, port);
    }


    public int getIpv4() {
        return ipv4;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "PeerResponse{" +
                "ipv4=" + ipv4 +
                ", port=" + port +
                '}';
    }
}
