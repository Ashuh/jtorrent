package jtorrent.peer.domain.model.message.typed;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.util.BitSet;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.IntStream;

public class Bitfield extends TypedPeerMessage {

    private final BitSet bitSet;

    public Bitfield(BitSet bitSet) {
        this.bitSet = requireNonNull(bitSet);
    }

    public static Bitfield forIndices(Collection<Integer> indices) {
        BitSet bitSet = new BitSet();
        indices.forEach(bitSet::set);
        return new Bitfield(bitSet);
    }

    public static Bitfield unpack(byte[] payload) {
        BitSet bitSet = new BitSet();

        for (int i = 0; i < payload.length; i++) {
            byte b = payload[i];
            int startIndex = i * Byte.SIZE;
            for (int j = 0; j < Byte.SIZE; j++) {
                if (isBitSet(b, j)) {
                    bitSet.set(startIndex + j);
                }
            }
        }

        return new Bitfield(bitSet);
    }

    /**
     * Checks whether the bit at the specified index is set.
     *
     * @param b        the byte to check
     * @param bitIndex the index of the bit to check. 0 is the leftmost bit.
     * @return true if the bit is set, false otherwise
     */
    private static boolean isBitSet(byte b, int bitIndex) {
        int mask = 1 << (Byte.SIZE - 1 - bitIndex);
        return (b & mask) != 0;
    }

    public IntStream getBits() {
        return bitSet.stream();
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Bitfield bitfield = (Bitfield) o;
        return bitSet.equals(bitfield.bitSet);
    }

    @Override
    public int getPayloadSize() {
        return (bitSet.length() + 7) / Byte.SIZE;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.BITFIELD;
    }

    @Override
    protected byte[] getPayload() {
        int numBytes = (bitSet.length() + Byte.SIZE - 1) / Byte.SIZE;
        byte[] bytes = new byte[numBytes];
        bitSet.stream().forEach(i -> {
            int byteIndex = i / Byte.SIZE;
            int bitIndex = i % Byte.SIZE;
            int mask = 1 << (Byte.SIZE - 1 - bitIndex);
            bytes[byteIndex] |= (byte) mask;
        });
        return bytes;
    }

    @Override
    protected String getPayloadString() {
        return String.format("bitSet=%s", bitSet);
    }
}
