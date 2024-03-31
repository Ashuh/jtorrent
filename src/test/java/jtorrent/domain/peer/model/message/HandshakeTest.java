package jtorrent.domain.peer.model.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import jtorrent.domain.common.util.Sha1Hash;

class HandshakeTest {

    @Test
    void unpack() {
        byte[] infoHashBytes = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        byte[] peerId = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        byte[] flags = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};

        Sha1Hash infoHash = new Sha1Hash(infoHashBytes);
        Handshake expected = new Handshake(infoHash, peerId, flags);

        byte[] handshakeBytes = ByteBuffer.allocate(68)
                .order(ByteOrder.BIG_ENDIAN)
                .put((byte) 19)
                .put("BitTorrent protocol".getBytes())
                .put(flags)
                .put(infoHashBytes)
                .put(peerId)
                .array();
        Handshake actual = Handshake.unpack(handshakeBytes);

        assertEquals(expected, actual);
    }

    @Test
    void pack() {
        byte[] flags = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] infoHashBytes = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        byte[] peerId = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        byte[] expected = ByteBuffer.allocate(68)
                .order(ByteOrder.BIG_ENDIAN)
                .put((byte) 19)
                .put("BitTorrent protocol".getBytes())
                .put(flags)
                .put(infoHashBytes)
                .put(peerId)
                .array();

        Sha1Hash infoHash = new Sha1Hash(infoHashBytes);
        Handshake handshake = new Handshake(infoHash, peerId, flags);
        byte[] actual = handshake.pack();

        assertArrayEquals(expected, actual);
    }
}
