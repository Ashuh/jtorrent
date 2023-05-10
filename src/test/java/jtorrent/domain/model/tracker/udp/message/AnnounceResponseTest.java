package jtorrent.domain.model.tracker.udp.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AnnounceResponseTest {

    @Test
    void unpack() {
        int numPeers = 2;
        int size = AnnounceResponse.PAYLOAD_MIN_BYTES + numPeers * PeerResponse.BYTES;
        ByteBuffer payload = ByteBuffer.allocate(size)
                .putInt(9999)
                .putInt(100)
                .putInt(10)
                .putInt(20);
        putPeerResponse(payload, 100, (short) 100);
        putPeerResponse(payload, 200, (short) 200);
        AnnounceResponse actual = AnnounceResponse.unpack(payload.array());

        List<PeerResponse> peerResponses = new ArrayList<>();
        peerResponses.add(new PeerResponse(100, 100));
        peerResponses.add(new PeerResponse(200, 200));
        AnnounceResponse expected = new AnnounceResponse(9999, 100, 10, 20, peerResponses);

        assertEquals(expected, actual);
    }

    private void putPeerResponse(ByteBuffer buffer, int ipv4, short port) {
        buffer.putInt(ipv4).putShort(port);
    }

    @Test
    void unpack_lessThanMinimumExpectedBytes_throwsIllegalArgumentException() {
        int size = 12;
        ByteBuffer payload = ByteBuffer.allocate(size)
                .putInt(9999)
                .putInt(100)
                .putInt(10);
        byte[] payloadBytes = payload.array();
        assertTrue(payloadBytes.length < AnnounceResponse.PAYLOAD_MIN_BYTES);
        assertThrows(IllegalArgumentException.class, () -> AnnounceResponse.unpack(payloadBytes));
    }
}
