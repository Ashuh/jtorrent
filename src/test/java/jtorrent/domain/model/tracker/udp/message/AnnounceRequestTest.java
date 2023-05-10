package jtorrent.domain.model.tracker.udp.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import jtorrent.domain.model.torrent.Sha1Hash;

class AnnounceRequestTest {

    @Test
    void pack() {
        Sha1Hash infoHash = new Sha1Hash(new byte[20]);
        byte[] peerId = "01234567890123456789".getBytes();
        AnnounceRequest announceRequest = new AnnounceRequest(
                9999,
                infoHash,
                peerId,
                1,
                2,
                3,
                Event.STARTED,
                1234,
                100,
                10,
                8000);
        byte[] actual = announceRequest.pack();

        byte[] expected = ByteBuffer.allocate(AnnounceRequest.BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(9999)
                .putInt(Action.ANNOUNCE.getValue())
                .putInt(announceRequest.transactionId)
                .put(infoHash.getBytes())
                .put(peerId)
                .putLong(1)
                .putLong(2)
                .putLong(3)
                .putInt(Event.STARTED.getValue())
                .putInt(1234)
                .putInt(100)
                .putInt(10)
                .putShort((short) 8000)
                .array();

        assertArrayEquals(expected, actual);
    }
}
