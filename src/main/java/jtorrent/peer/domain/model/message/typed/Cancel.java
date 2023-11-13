package jtorrent.peer.domain.model.message.typed;

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
    public MessageType getMessageType() {
        return MessageType.CANCEL;
    }
}
