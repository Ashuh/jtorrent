package jtorrent.domain.peer.model.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class KeepAliveTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(0)
                .array();

        KeepAlive keepAlive = new KeepAlive();
        byte[] actual = keepAlive.pack();

        assertArrayEquals(expected, actual);
    }
}
