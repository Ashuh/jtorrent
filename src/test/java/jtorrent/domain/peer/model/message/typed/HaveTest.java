package jtorrent.domain.peer.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class HaveTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(9)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(5)
                .put(MessageType.HAVE.getValue())
                .putInt(1)
                .array();

        Have have = new Have(1);
        byte[] actual = have.pack();

        assertArrayEquals(expected, actual);
    }

    @Test
    void unpack() {
        Have expected = new Have(1);

        byte[] payload = ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .array();
        Have actual = Have.unpack(payload);

        assertEquals(expected, actual);
    }
}
