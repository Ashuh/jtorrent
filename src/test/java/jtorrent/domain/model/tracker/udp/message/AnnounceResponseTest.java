package jtorrent.domain.model.tracker.udp.message;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jtorrent.domain.model.exception.UnpackException;

class AnnounceResponseTest {

    @Test
    void unpack() {
        int numPeers = 2;
        int size = 16 + numPeers * 6;
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
    void unpack_lessThanMinimumExpectedBytes_throwsUnpackException() {
        ByteBuffer payload = ByteBuffer.allocate(15);
        byte[] payloadBytes = payload.array();
        assertThrows(UnpackException.class, () -> AnnounceResponse.unpack(payloadBytes));
    }

    @Test
    void unpack_equalToMinimumExpectedBytes_doesNotThrow() {
        ByteBuffer payload = ByteBuffer.allocate(16);
        byte[] payloadBytes = payload.array();
        assertDoesNotThrow(() -> AnnounceResponse.unpack(payloadBytes));
    }
}
