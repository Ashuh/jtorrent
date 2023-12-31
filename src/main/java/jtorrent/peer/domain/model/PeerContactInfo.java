package jtorrent.peer.domain.model;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class PeerContactInfo {

    private static final int COMPACT_PEER_INFO_BYTES = 6;

    private final InetAddress address;
    private final int port;

    public PeerContactInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public static PeerContactInfo fromCompactPeerInfo(byte[] bytes) {
        if (bytes.length != COMPACT_PEER_INFO_BYTES) {
            throw new IllegalArgumentException(
                    String.format("Compact peer info must be %d bytes long", COMPACT_PEER_INFO_BYTES));
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        byte[] addressBytes = new byte[Integer.BYTES];
        buffer.get(addressBytes);
        InetAddress address;
        try {
            address = InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }

        int port = Short.toUnsignedInt(buffer.getShort());
        return new PeerContactInfo(address, port);
    }

    public static PeerContactInfo fromString(String ipPort) throws UnknownHostException {
        String[] split = ipPort.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid IP:PORT format");
        }
        String ip = split[0];
        int port;
        try {
            port = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid IP:PORT format", e);
        }
        InetAddress address = InetAddress.getByName(ip);
        return new PeerContactInfo(address, port);
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress toInetSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    public byte[] toCompactPeerInfo() {
        return ByteBuffer.allocate(6)
                .put(address.getAddress())
                .putShort((short) port)
                .array();
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PeerContactInfo that = (PeerContactInfo) o;
        return port == that.port && Objects.equals(address, that.address);
    }

    @Override
    public String toString() {
        return String.format("%s:%d", address.getHostAddress(), port);
    }
}
