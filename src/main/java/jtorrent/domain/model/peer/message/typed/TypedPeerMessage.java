package jtorrent.domain.model.peer.message.typed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jtorrent.domain.model.peer.message.PeerMessage;

public abstract class TypedPeerMessage implements PeerMessage {

    public byte[] pack() {
        byte[] payload = getPayload();
        // total size of the message in bytes
        int sizeInBytes = payload.length + Integer.BYTES + Byte.BYTES;
        // size of the payload in bytes + 1 byte for the message type
        int lengthPrefix = payload.length + Byte.BYTES;

        return ByteBuffer.allocate(sizeInBytes)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(lengthPrefix)
                .put(getMessageType().getValue())
                .put(getPayload())
                .array();
    }

    public abstract MessageType getMessageType();

    protected abstract byte[] getPayload();
}
