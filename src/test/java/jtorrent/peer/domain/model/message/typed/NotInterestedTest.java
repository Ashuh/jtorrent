package jtorrent.peer.domain.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class NotInterestedTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(5)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .put(MessageType.NOT_INTERESTED.getValue())
                .array();

        NotInterested notInterested = new NotInterested();
        byte[] actual = notInterested.pack();

        assertArrayEquals(expected, actual);
    }
}
