package jtorrent.domain.peer.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class InterestedTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(5)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .put(MessageType.INTERESTED.getValue())
                .array();

        Interested interested = new Interested();
        byte[] actual = interested.pack();

        assertArrayEquals(expected, actual);
    }
}
