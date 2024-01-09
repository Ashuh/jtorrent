package jtorrent.domain.dht.model.node;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jtorrent.domain.common.util.Bit160Value;

class NodeIdTest {

    @Test
    void fromBigInteger_null_throwsIllegalArgumentException() {
        assertThrowsExactly(IllegalArgumentException.class, () -> NodeId.fromBigInteger(null));
    }

    @Test
    void fromBigInteger_negative_throwsIllegalArgumentException() {
        BigInteger negativeValue = BigInteger.valueOf(-1);
        assertEquals(-1, negativeValue.signum());
        assertThrowsExactly(IllegalArgumentException.class, () -> NodeId.fromBigInteger(negativeValue));
    }

    @Test
    void fromBigInteger_longerThan160Bits_throwsIllegalArgumentException() {
        BigInteger value = BigInteger.TWO.pow(Bit160Value.SIZE_BITS);
        assertTrue(value.bitLength() > Bit160Value.SIZE_BITS);
        assertThrowsExactly(IllegalArgumentException.class, () -> NodeId.fromBigInteger(value));
    }

    @Test
    void fromBigInteger_equalTo160Bits_success() {
        BigInteger value = BigInteger.TWO.pow(Bit160Value.SIZE_BITS).subtract(BigInteger.ONE);
        assertEquals(Bit160Value.SIZE_BITS, value.bitLength());
        assertDoesNotThrow(() -> NodeId.fromBigInteger(value));
    }

    @Test
    void fromBigInteger_shorterThan160Bits_success() {
        BigInteger zero = BigInteger.ZERO;
        assertTrue(zero.bitLength() < Bit160Value.SIZE_BITS);
        assertDoesNotThrow(() -> NodeId.fromBigInteger(zero));
    }

    @Test
    void fromBigIntegerToBigInteger_returnsSameValue() {
        BigInteger expected1 = BigInteger.ZERO;
        BigInteger actual1 = NodeId.fromBigInteger(expected1).toBigInteger();
        assertEquals(expected1, actual1);

        BigInteger expected2 = BigInteger.ONE;
        BigInteger actual2 = NodeId.fromBigInteger(expected2).toBigInteger();
        assertEquals(expected2, actual2);

        BigInteger expected3 = BigInteger.TWO.pow(Bit160Value.SIZE_BITS).subtract(BigInteger.ONE);
        BigInteger actual3 = NodeId.fromBigInteger(expected3).toBigInteger();
        assertEquals(expected3, actual3);
    }

    @Test
    void distanceTo_differentId() {
        NodeId id1 = new NodeIdBuilder().setBit(159).build();
        assertEquals(BigInteger.ONE, NodeId.ZERO.distanceTo(id1));

        NodeId id2 = new NodeIdBuilder().setBit(158).build();
        assertEquals(BigInteger.TWO, NodeId.ZERO.distanceTo(id2));

        NodeId id3 = new NodeIdBuilder().setBit(156, 158).build();
        assertEquals(BigInteger.TEN, NodeId.ZERO.distanceTo(id3));

        NodeId id4 = new NodeIdBuilder().setBit(0).build();
        assertEquals(BigInteger.TWO.pow(159), NodeId.ZERO.distanceTo(id4));

        NodeId id5 = new NodeIdBuilder().setBit(1).build();
        assertEquals(BigInteger.TWO.pow(158), NodeId.ZERO.distanceTo(id5));

        NodeId id6 = new NodeIdBuilder().setBit(0, 1).build();
        BigInteger expectedDistance6 = BigInteger.TWO.pow(159).add(BigInteger.TWO.pow(158));
        assertEquals(expectedDistance6, NodeId.ZERO.distanceTo(id6));

        NodeId id7 = new NodeIdBuilder().setAll().clearBit(0).build();
        assertEquals(BigInteger.TWO.pow(159), NodeId.MAX.distanceTo(id7));

        NodeId id8 = new NodeIdBuilder().setAll().clearBit(2).build();
        BigInteger expectedDistance8 = BigInteger.TWO.pow(157);
        assertEquals(expectedDistance8, NodeId.MAX.distanceTo(id8));

        NodeId id9 = new NodeIdBuilder().setAll().clearBit(60).build();
        BigInteger expectedDistance9 = BigInteger.TWO.pow(99);
        assertEquals(expectedDistance9, NodeId.MAX.distanceTo(id9));

        NodeId id10 = new NodeIdBuilder().setAll().clearBit(159).build();
        assertEquals(BigInteger.ONE, NodeId.MAX.distanceTo(id10));

        BigInteger expectedMaxDistance = BigInteger.TWO.pow(160).subtract(BigInteger.ONE);
        assertEquals(expectedMaxDistance, NodeId.ZERO.distanceTo(NodeId.MAX));
    }

    @Test
    void distanceTo_sameId_returnsZero() {
        assertEquals(BigInteger.ZERO, NodeId.ZERO.distanceTo(NodeId.ZERO));
        assertEquals(BigInteger.ZERO, NodeId.MAX.distanceTo(NodeId.MAX));

        NodeId id1 = new NodeIdBuilder().setBit(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).build();
        NodeId id2 = new NodeIdBuilder().setBit(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).build();
        assertEquals(BigInteger.ZERO, id1.distanceTo(id2));
    }

    @Test
    void equals_sameObject_returnsTrue() {
        assertEquals(NodeId.ZERO, NodeId.ZERO);
        assertEquals(NodeId.MAX, NodeId.MAX);
    }

    @Test
    void equals_sameId_returnsTrue() {
        NodeId id1 = new NodeIdBuilder().setBit(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).build();
        NodeId id2 = new NodeIdBuilder().setBit(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).build();
        assertEquals(id1, id2);
    }

    @Test
    void equals_differentId_returnsFalse() {
        assertNotEquals(NodeId.ZERO, NodeId.MAX);

        NodeId id1 = new NodeIdBuilder().setBit(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).build();
        NodeId id2 = new NodeIdBuilder().setBit(0, 1, 2, 3, 4, 5, 6, 7, 8).build();
        assertNotEquals(id1, id2);
    }

    @Test
    void equals_null_returnsFalse() {
        assertNotEquals(null, NodeId.ZERO);
        assertNotEquals(null, NodeId.MAX);
    }


    private static class NodeIdBuilder {

        private final Set<Integer> set = new HashSet<>();

        public NodeIdBuilder setBit(int index) {
            if (index < 0 || index >= Bit160Value.SIZE_BITS) {
                throw new IllegalArgumentException("Index must be between 0 and 159");
            }
            set.add(index);
            return this;
        }

        public NodeIdBuilder setBit(int... indexes) {
            Arrays.stream(indexes).forEach(this::setBit);
            return this;
        }

        public NodeIdBuilder setAll() {
            for (int i = 0; i < Bit160Value.SIZE_BITS; i++) {
                setBit(i);
            }
            return this;
        }

        public NodeIdBuilder clearBit(int index) {
            if (index < 0 || index >= Bit160Value.SIZE_BITS) {
                throw new IllegalArgumentException("Index must be between 0 and 159");
            }
            set.remove(index);
            return this;
        }

        public NodeIdBuilder clearBit(int... indexes) {
            Arrays.stream(indexes).forEach(this::clearBit);
            return this;
        }

        public NodeIdBuilder clearAll() {
            set.clear();
            return this;
        }

        public NodeId build() {
            byte[] id = new byte[20];
            set.forEach(i -> id[i / 8] |= 1 << (7 - i % 8));
            return new NodeId(id);
        }
    }
}
