package jtorrent.domain.model.tracker.udp.message;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import jtorrent.domain.model.exception.UnpackException;

class ConnectionResponseTest {

    @Test
    void unpack() {
        ConnectionResponse expected = new ConnectionResponse(1111, 123456789);

        byte[] payload = ByteBuffer.allocate(12)
                .putInt(1111)
                .putLong(123456789)
                .array();
        ConnectionResponse actual = ConnectionResponse.unpack(payload);

        assertEquals(expected, actual);
    }

    @Test
    void unpack_lessThanExpectedBytes_throwsUnpackException() {
        byte[] payload = ByteBuffer.allocate(11).array();
        assertThrows(UnpackException.class, () -> ConnectionResponse.unpack(payload));
    }
}
