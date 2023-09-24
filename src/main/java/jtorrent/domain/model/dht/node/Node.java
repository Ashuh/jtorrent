package jtorrent.domain.model.dht.node;

import static java.util.Objects.requireNonNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Node {

    public static final int COMPACT_NODE_INFO_BYTES = 26;

    private final NodeId id;
    private final InetSocketAddress address;

    public Node(NodeId id, InetSocketAddress address) {
        this.address = requireNonNull(address);
        this.id = requireNonNull(id);
    }

    public static Collection<Node> multipleFromCompactNodeInfo(byte[] bytes) {
        if (bytes.length % Node.COMPACT_NODE_INFO_BYTES != 0) {
            throw new IllegalArgumentException(
                    String.format("Bytes length must be a multiple of %d", Node.COMPACT_NODE_INFO_BYTES));
        }

        int numNodes = bytes.length / Node.COMPACT_NODE_INFO_BYTES;
        return IntStream.range(0, numNodes)
                .map(i -> i * Node.COMPACT_NODE_INFO_BYTES)
                .mapToObj(from -> Arrays.copyOfRange(bytes, from, from + Node.COMPACT_NODE_INFO_BYTES))
                .map(Node::fromCompactNodeInfo)
                .collect(Collectors.toList());
    }

    /**
     * Unpacks a node from its compact node info representation.
     * The compact node info representation is a 26-byte string consisting of the node ID (20 bytes), followed by the IP
     * address (4 bytes in network byte order), followed by the port number (2 bytes in network byte order).
     *
     * @param bytes the compact node info representation
     * @return the unpacked node
     * @throws IllegalArgumentException if bytes is not 26 bytes long
     */
    public static Node fromCompactNodeInfo(byte[] bytes) {
        if (bytes.length != COMPACT_NODE_INFO_BYTES) {
            throw new IllegalArgumentException(
                    String.format("Compact node info must be %d bytes long", COMPACT_NODE_INFO_BYTES));
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        byte[] idBytes = new byte[20];
        buffer.get(idBytes);
        NodeId id = new NodeId(idBytes);

        byte[] addressBytes = new byte[Integer.BYTES];
        buffer.get(addressBytes);
        InetAddress address;
        try {
            address = InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }

        int port = Short.toUnsignedInt(buffer.getShort());
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        return new Node(id, socketAddress);
    }

    public static byte[] multipleToCompactNodeInfo(Collection<Node> nodes) {
        ByteBuffer buffer = ByteBuffer.allocate(nodes.size() * COMPACT_NODE_INFO_BYTES);
        nodes.stream()
                .map(Node::toCompactNodeInfo)
                .forEach(buffer::put);
        return buffer.array();
    }

    public byte[] toCompactNodeInfo() {
        return ByteBuffer.allocate(COMPACT_NODE_INFO_BYTES)
                .put(id.getBytes())
                .put(address.getAddress().getAddress())
                .putShort((short) address.getPort())
                .array();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public NodeId getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node) o;
        return Objects.equals(address, node.address) && Objects.equals(id, node.id);
    }

    @Override
    public String toString() {
        return String.format("%s @ %s", id, address);
    }
}