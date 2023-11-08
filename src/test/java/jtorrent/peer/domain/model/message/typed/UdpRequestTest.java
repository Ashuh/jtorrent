package jtorrent.peer.domain.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class UdpRequestTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(17)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(13)
                .put(MessageType.REQUEST.getValue())
                .putInt(1)
                .putInt(2)
                .putInt(3)
                .array();

        Request request = new Request(1, 2, 3);
        byte[] actual = request.pack();

        assertArrayEquals(expected, actual);
    }

    @Test
    void unpack() {
        Request expected = new Request(1, 2, 3);

        byte[] payload = ByteBuffer.allocate(12)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .putInt(2)
                .putInt(3)
                .array();
        Request actual = Request.unpack(payload);

        assertEquals(expected, actual);
    }
}
