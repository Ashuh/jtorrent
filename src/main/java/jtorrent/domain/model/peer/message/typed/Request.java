package jtorrent.domain.model.peer.message.typed;

import java.nio.ByteBuffer;

public class Request extends BlockMessage {

    public Request(int index, int begin, int length) {
        super(index, begin, length);
    }

    public static Request unpack(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int index = buffer.getInt();
        int begin = buffer.getInt();
        int length = buffer.getInt();
        return new Request(index, begin, length);
    }

    @Override
    protected MessageType getMessageType() {
        return MessageType.REQUEST;
    }

    @Override
    public String toString() {
        return "Request{"
                + "index=" + index
                + ", begin=" + begin
                + ", length=" + length
                + '}';
    }
}
