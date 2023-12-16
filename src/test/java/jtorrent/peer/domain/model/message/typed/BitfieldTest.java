package jtorrent.peer.domain.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

import org.junit.jupiter.api.Test;

class BitfieldTest {

    @Test
    void pack() {
        BitSet bitSet = new BitSet(8);
        bitSet.set(0);
        bitSet.set(2);
        bitSet.set(4);
        bitSet.set(6);
        bitSet.set(7);
        bitSet.set(8);

        byte[] payload = new byte[] {(byte) 0b1010_1011, (byte) 0b1000_0000};
        byte[] expected = ByteBuffer.allocate(7)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(3)
                .put(MessageType.BITFIELD.getValue())
                .put(payload)
                .array();

        Bitfield bitfield = Bitfield.fromBitSetAndNumTotalPieces(bitSet, 9);
        byte[] actual = bitfield.pack();

        assertArrayEquals(expected, actual);
    }

    @Test
    void unpack() {
        BitSet bitSet = new BitSet();
        bitSet.set(0);
        bitSet.set(2);
        bitSet.set(4);
        bitSet.set(6);
        bitSet.set(7);
        bitSet.set(8);

        Bitfield expected = Bitfield.fromBitSetAndNumTotalPieces(bitSet, 9);

        byte[] payload = new byte[] {(byte) 0b1010_1011, (byte) 0b1000_0000};
        Bitfield actual = Bitfield.unpack(payload);

        assertEquals(expected, actual);
    }
}
