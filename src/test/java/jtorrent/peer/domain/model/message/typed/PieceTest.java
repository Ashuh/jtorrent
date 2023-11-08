package jtorrent.peer.domain.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class PieceTest {

    @Test
    void pack() {
        byte[] block = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        byte[] expected = ByteBuffer.allocate(23)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(19)
                .put(MessageType.PIECE.getValue())
                .putInt(1)
                .putInt(2)
                .put(block)
                .array();

        Piece piece = new Piece(1, 2, block);
        byte[] actual = piece.pack();

        assertArrayEquals(expected, actual);
    }

    @Test
    void unpack() {
        byte[] block = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        Piece expected = new Piece(1, 2, block);

        byte[] payload = ByteBuffer.allocate(18)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .putInt(2)
                .put(block)
                .array();
        Piece actual = Piece.unpack(payload);

        assertEquals(expected, actual);
    }
}
