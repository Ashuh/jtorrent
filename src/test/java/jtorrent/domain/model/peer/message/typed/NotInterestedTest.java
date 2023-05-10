package jtorrent.domain.model.peer.message.typed;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class NotInterestedTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(5)
                .putInt(1)
                .put(MessageType.NOT_INTERESTED.getValue())
                .array();

        NotInterested notInterested = new NotInterested();
        byte[] actual = notInterested.pack();

        assertArrayEquals(expected, actual);
    }
}
