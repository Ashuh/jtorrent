package jtorrent.tracker.domain.model.udp.message.response;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jtorrent.common.domain.exception.UnpackException;

class UdpAnnounceResponseTest {

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
        UdpAnnounceResponse actual = UdpAnnounceResponse.unpack(payload.array());

        List<UdpPeerResponse> udpPeerRespons = new ArrayList<>();
        udpPeerRespons.add(new UdpPeerResponse(100, 100));
        udpPeerRespons.add(new UdpPeerResponse(200, 200));
        UdpAnnounceResponse expected = new UdpAnnounceResponse(9999, 100, 10, 20, udpPeerRespons);

        assertEquals(expected, actual);
    }

    private void putPeerResponse(ByteBuffer buffer, int ipv4, short port) {
        buffer.putInt(ipv4).putShort(port);
    }

    @Test
    void unpack_lessThanMinimumExpectedBytes_throwsUnpackException() {
        ByteBuffer payload = ByteBuffer.allocate(15);
        byte[] payloadBytes = payload.array();
        assertThrows(UnpackException.class, () -> UdpAnnounceResponse.unpack(payloadBytes));
    }

    @Test
    void unpack_equalToMinimumExpectedBytes_doesNotThrow() {
        ByteBuffer payload = ByteBuffer.allocate(16);
        byte[] payloadBytes = payload.array();
        assertDoesNotThrow(() -> UdpAnnounceResponse.unpack(payloadBytes));
    }
}
