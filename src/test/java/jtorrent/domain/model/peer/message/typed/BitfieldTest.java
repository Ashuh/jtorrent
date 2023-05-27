package jtorrent.domain.model.peer.message.typed;

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

        byte[] expected = ByteBuffer.allocate(6)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(2)
                .put(MessageType.BITFIELD.getValue())
                .put(bitSet.toByteArray())
                .array();

        Bitfield bitfield = new Bitfield(bitSet);
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

        Bitfield expected = new Bitfield(bitSet);

        byte[] payload = new byte[2];

        bitSet.stream().forEach(i -> {
            int byteIndex = i / Byte.SIZE;
            payload[byteIndex] |= 1 << (7 - i % Byte.SIZE);
        });

        Bitfield actual = Bitfield.unpack(payload);

        assertEquals(expected, actual);
    }
}
