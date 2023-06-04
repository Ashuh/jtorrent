package jtorrent.domain.model.tracker.udp.message.request;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import jtorrent.domain.model.tracker.udp.message.Action;
import jtorrent.domain.model.tracker.udp.message.request.UdpConnectionRequest;

class UdpConnectionRequestTest {

    @Test
    void pack() {
        UdpConnectionRequest connectionRequest = new UdpConnectionRequest();
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
