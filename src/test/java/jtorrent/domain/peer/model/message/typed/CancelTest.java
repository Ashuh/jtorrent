package jtorrent.domain.peer.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class CancelTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(17)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(13)
                .put(MessageType.CANCEL.getValue())
                .putInt(1)
                .putInt(2)
                .putInt(3)
                .array();

        Cancel cancel = new Cancel(1, 2, 3);
        byte[] actual = cancel.pack();

        assertArrayEquals(expected, actual);
    }

    @Test
    void unpack() {
        Cancel expected = new Cancel(1, 2, 3);

        byte[] payload = ByteBuffer.allocate(12)
                .putInt(1)
                .putInt(2)
                .putInt(3)
                .array();
        Cancel actual = Cancel.unpack(payload);

        assertEquals(expected, actual);
    }
}
