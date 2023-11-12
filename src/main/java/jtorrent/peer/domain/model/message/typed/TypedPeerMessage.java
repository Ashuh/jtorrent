package jtorrent.peer.domain.model.message.typed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jtorrent.peer.domain.model.message.PeerMessage;

public abstract class TypedPeerMessage implements PeerMessage {

    protected static final int LENGTH_PREFIX_SIZE = Integer.BYTES;
    private static final int MESSAGE_TYPE_SIZE = Byte.BYTES;
    /**
     * Size of the message header in bytes.
     * <p>
     * The header consists of a 4-byte length prefix indicating the length of the message in bytes
     * (excluding the length prefix itself) and a 1-byte message type code.
     */
    protected static final int HEADER_SIZE = LENGTH_PREFIX_SIZE + MESSAGE_TYPE_SIZE;

    public byte[] pack() {
        return ByteBuffer.allocate(getMessageSize())
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(getLengthPrefix())
                .put(getMessageType().getValue())
                .put(getPayload())
                .array();
    }

    @Override
    public int getMessageSize() {
        return getPayloadSize() + HEADER_SIZE;
    }

    /**
     * Gets the size of the payload in bytes.
     */
    protected abstract int getPayloadSize();

    private int getLengthPrefix() {
        return getPayloadSize() + MESSAGE_TYPE_SIZE;
    }

    public abstract MessageType getMessageType();

    protected abstract byte[] getPayload();
}
