package jtorrent.domain.tracker.model.udp.message.request;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import jtorrent.domain.tracker.model.udp.message.Action;

class UdpConnectionRequestTest {

    @Test
    void pack() {
        UdpConnectionRequest connectionRequest = new UdpConnectionRequest();
        byte[] actual = connectionRequest.pack();

        byte[] expected = ByteBuffer.allocate(16)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(0x41727101980L)
                .putInt(Action.CONNECT.getValue())
                .putInt(connectionRequest.getTransactionId())
                .array();

        assertArrayEquals(expected, actual);
    }
}
