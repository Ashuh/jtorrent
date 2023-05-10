package jtorrent.domain.model.peer.message.typed;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

public class Bitfield extends TypedPeerMessage {

    private final BitSet bitSet;

    public Bitfield(BitSet bitSet) {
        this.bitSet = requireNonNull(bitSet);
    }

    public static Bitfield unpack(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] block = new byte[buffer.remaining()];
        buffer.get(block);
        BitSet bitSet = BitSet.valueOf(block);
        return new Bitfield(bitSet);
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
    public String toString() {
        return "Bitfield{" +
                "bitSet=" + bitSet +
                '}';
    }

    @Override
    protected MessageType getMessageType() {
        return MessageType.BITFIELD;
    }

    @Override
    protected byte[] getPayload() {
        return bitSet.toByteArray();
    }
}
