package jtorrent.tracker.domain.model.udp.message.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import jtorrent.common.domain.exception.UnpackException;

class UdpConnectionResponseTest {

    @Test
    void unpack() {
        UdpConnectionResponse expected = new UdpConnectionResponse(1111, 123456789);

        byte[] payload = ByteBuffer.allocate(12)
                .putInt(1111)
                .putLong(123456789)
                .array();
        UdpConnectionResponse actual = UdpConnectionResponse.unpack(payload);

        assertEquals(expected, actual);
    }

    @Test
    void unpack_lessThanExpectedBytes_throwsUnpackException() {
        byte[] payload = ByteBuffer.allocate(11).array();
        assertThrows(UnpackException.class, () -> UdpConnectionResponse.unpack(payload));
    }
}
