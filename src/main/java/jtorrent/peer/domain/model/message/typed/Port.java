package jtorrent.peer.domain.model.message.typed;

import java.nio.ByteBuffer;
import java.util.Objects;

public class Port extends TypedPeerMessage {

    private static final int PORT_MIN_VALUE = 0;
    private static final int PORT_MAX_VALUE = 65535;

    /**
     * The port number that the peer's DHT node is listening on.
     */
    private final int listenPort;

    public Port(int listenPort) {
        if (!isValidPort(listenPort)) {
            throw new IllegalArgumentException("Invalid port: " + listenPort);
        }
        this.listenPort = listenPort;
    }

    private static boolean isValidPort(int port) {
        return port >= PORT_MIN_VALUE && port <= PORT_MAX_VALUE;
    }

    public static Port unpack(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int port = Short.toUnsignedInt(buffer.getShort());
        return new Port(port);
    }

    public int getListenPort() {
        return listenPort;
    }

    @Override
    protected int getPayloadSize() {
        return Short.BYTES;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.PORT;
    }

    @Override
    protected byte[] getPayload() {
        return ByteBuffer.allocate(Short.BYTES)
                .putShort((short) listenPort)
                .array();
    }

    @Override
    public int hashCode() {
        return Objects.hash(listenPort);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Port port = (Port) o;
        return listenPort == port.listenPort;
    }

    @Override
    public String toString() {
        return "Port{"
                + "listenPort=" + listenPort
                + '}';
    }
}
