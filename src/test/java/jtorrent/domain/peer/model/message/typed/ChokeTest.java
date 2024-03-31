package jtorrent.domain.peer.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class ChokeTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(5)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .put(MessageType.CHOKE.getValue())
                .array();

        Choke choke = new Choke();
        byte[] actual = choke.pack();

        assertArrayEquals(expected, actual);
    }
}
