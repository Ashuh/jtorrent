package jtorrent.domain.model.peer.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class UnchokeTest {

    @Test
    void pack() {
        byte[] expected = ByteBuffer.allocate(5)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .put(MessageType.UNCHOKE.getValue())
                .array();

        Unchoke unchoke = new Unchoke();
        byte[] actual = unchoke.pack();

        assertArrayEquals(expected, actual);
    }
}
