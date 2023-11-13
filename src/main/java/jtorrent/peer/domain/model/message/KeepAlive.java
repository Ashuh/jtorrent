package jtorrent.peer.domain.model.message;

import java.nio.ByteBuffer;

public class KeepAlive implements PeerMessage {

    private static final int BYTES = 4;

    @Override
    public byte[] pack() {
        return ByteBuffer.allocate(BYTES)
                .putInt(0)
                .array();
    }

    @Override
    public int getMessageSize() {
        return BYTES;
    }

    @Override
    public String toString() {
        return "[KEEP ALIVE]";
    }
}
