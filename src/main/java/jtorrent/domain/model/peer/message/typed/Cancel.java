package jtorrent.domain.model.peer.message.typed;

import java.nio.ByteBuffer;

public class Cancel extends BlockMessage {

    public Cancel(int index, int begin, int length) {
        super(index, begin, length);
    }

    public static Cancel unpack(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int index = buffer.getInt();
        int begin = buffer.getInt();
        int length = buffer.getInt();
        return new Cancel(index, begin, length);
    }

    @Override
    protected MessageType getMessageType() {
        return MessageType.CANCEL;
    }

    @Override
    public String toString() {
        return "Cancel{" +
                "index=" + index +
                ", begin=" + begin +
                ", length=" + length +
                '}';
    }
}
