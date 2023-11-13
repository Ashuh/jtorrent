package jtorrent.peer.domain.model.message.typed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class Have extends TypedPeerMessage {

    private static final int PAYLOAD_BYTES = 4;

    private final int pieceIndex;

    public Have(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    public static Have unpack(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int pieceIndex = buffer.getInt();
        return new Have(pieceIndex);
    }

    @Override
    protected int getPayloadSize() {
        return PAYLOAD_BYTES;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.HAVE;
    }

    @Override
    protected byte[] getPayload() {
        return ByteBuffer.allocate(PAYLOAD_BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(pieceIndex)
                .array();
    }

    @Override
    protected String getPayloadString() {
        return String.format("pieceIndex=%d", pieceIndex);
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Have have = (Have) o;
        return pieceIndex == have.pieceIndex;
    }
}
