package jtorrent.domain.model.tracker.udp.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class ConnectionRequestTest {

    @Test
    void pack() {
        ConnectionRequest connectionRequest = new ConnectionRequest();
        byte[] actual = connectionRequest.pack();

        byte[] expected = ByteBuffer.allocate(16)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(0x41727101980L)
                .putInt(Action.CONNECT.getValue())
                .putInt(connectionRequest.transactionId)
                .array();

        assertArrayEquals(expected, actual);
    }
}
