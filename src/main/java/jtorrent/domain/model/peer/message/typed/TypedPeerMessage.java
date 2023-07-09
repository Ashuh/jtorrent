package jtorrent.domain.model.peer.message.typed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jtorrent.domain.model.peer.message.PeerMessage;

public abstract class TypedPeerMessage implements PeerMessage {

    public byte[] pack() {
        return ByteBuffer.allocate(getSizeInBytes())
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(getLengthPrefix())
                .put(getMessageType().getValue())
                .put(getPayload())
                .array();
    }

    private int getSizeInBytes() {
        return getPayload().length
                + Integer.BYTES
                + Byte.BYTES;
    }

    private int getLengthPrefix() {
        return getPayload().length + Byte.BYTES;
    }

    public abstract MessageType getMessageType();

    protected abstract byte[] getPayload();
}
