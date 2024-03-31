package jtorrent.domain.peer.model.message.typed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PortTest {

    private static final int PORT_MIN_VALUE = 0;
    private static final int PORT_MAX_VALUE = 65535;
    private static final byte[] PORT_MAX_VALUE_BYTES = {(byte) 0b11111111, (byte) 0b11111111};

    @BeforeAll
    static void setUp() {
        assertEquals(PORT_MAX_VALUE, Short.toUnsignedInt(ByteBuffer.wrap(PORT_MAX_VALUE_BYTES).getShort()));
    }

    @Test
    void pack() {
        int listenPort = 6881;

        byte[] expected = ByteBuffer.allocate(7)
                .putInt(3)
                .put((byte) 9)
                .putShort((short) listenPort)
                .array();

        Port port = new Port(listenPort);
        byte[] actual = port.pack();

        assertArrayEquals(expected, actual);

        // Test min port value
        byte[] minPortExpected = ByteBuffer.allocate(7)
                .putInt(3)
                .put((byte) 9)
                .putShort((short) PORT_MIN_VALUE)
                .array();

        Port minPort = new Port(PORT_MIN_VALUE);
        byte[] minPortActual = minPort.pack();

        assertArrayEquals(minPortExpected, minPortActual);

        // Test max port value
        byte[] maxPortExpected = ByteBuffer.allocate(7)
                .putInt(3)
                .put((byte) 9)
                .put(PORT_MAX_VALUE_BYTES)
                .array();

        Port maxPort = new Port(PORT_MAX_VALUE);
        byte[] maxPortActual = maxPort.pack();

        assertArrayEquals(maxPortExpected, maxPortActual);
    }

    @Test
    void unpack() {
        int listenPort = 6881;

        Port expected = new Port(listenPort);

        byte[] payload = ByteBuffer.allocate(2)
                .putShort((short) listenPort)
                .array();
        Port actual = Port.unpack(payload);

        assertEquals(expected, actual);

        // Test min port value
        Port minPortExpected = new Port(PORT_MIN_VALUE);

        byte[] minPortPayload = ByteBuffer.allocate(2)
                .putShort((short) PORT_MIN_VALUE)
                .array();
        Port minPortActual = Port.unpack(minPortPayload);

        assertEquals(minPortExpected, minPortActual);

        // Test max port value
        Port maxPortExpected = new Port(PORT_MAX_VALUE);

        byte[] maxPortPayload = ByteBuffer.allocate(2)
                .put(PORT_MAX_VALUE_BYTES)
                .array();
        Port maxPortActual = Port.unpack(maxPortPayload);

        assertEquals(maxPortExpected, maxPortActual);
    }

    @Test
    void packUnpack() {
        Port expected = new Port(6881);
        byte[] payload = extractPayload(expected.pack());

        Port actual = Port.unpack(payload);
        assertEquals(expected, actual);

        Port minPortExpected = new Port(PORT_MIN_VALUE);
        byte[] minPortPayload = extractPayload(minPortExpected.pack());
        Port minPortActual = Port.unpack(minPortPayload);
        assertEquals(minPortExpected, minPortActual);

        Port maxPortExpected = new Port(PORT_MAX_VALUE);
        byte[] maxPortPayload = extractPayload(maxPortExpected.pack());
        Port maxPortActual = Port.unpack(maxPortPayload);
        assertEquals(maxPortExpected, maxPortActual);
    }

    private byte[] extractPayload(byte[] packed) {
        return Arrays.copyOfRange(packed, 5, packed.length);
    }
}
