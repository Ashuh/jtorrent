package jtorrent.domain.model.dht.message.response;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.model.dht.node.NodeId;

public class FindNodeResponse extends DefinedResponse {

    private static final String KEY_NODES = "nodes";

    private final Collection<Node> nodes;

    public FindNodeResponse(NodeId id, Collection<Node> nodes) {
        super(id);
        this.nodes = nodes;
    }

    public FindNodeResponse(TransactionId transactionId, NodeId id, Collection<Node> nodes) {
        super(transactionId, id);
        this.nodes = nodes;
    }

    public FindNodeResponse(TransactionId transactionId, String clientVersion, NodeId id, Collection<Node> nodes) {
        super(transactionId, clientVersion, id);
        this.nodes = nodes;
    }

    public static FindNodeResponse fromUnknownResponse(UndefinedResponse response) {
        TransactionId transactionId = response.getTransactionId();

        byte[] nodeIdBytes = response.getReturnValues().getBytes(KEY_ID).array();
        NodeId nodeId = new NodeId(nodeIdBytes);

        byte[] nodesBytes = response.getReturnValues().getBytes(KEY_NODES).array();
        Collection<Node> nodes = Node.multipleFromCompactNodeInfo(nodesBytes);

        return new FindNodeResponse(transactionId, nodeId, nodes);
    }

    //    private static Collection<Node> unpackNodes(byte[] bytes) {
    //        if (bytes.length % Node.COMPACT_NODE_INFO_BYTES != 0) {
    //            throw new IllegalArgumentException(
    //                    String.format("Compact node info must be %d bytes long", Node.COMPACT_NODE_INFO_BYTES));
    //        }
    //
    //        int numNodes = bytes.length / Node.COMPACT_NODE_INFO_BYTES;
    //        return IntStream.range(0, numNodes)
    //                .map(i -> i * Node.COMPACT_NODE_INFO_BYTES)
    //                .mapToObj(from -> Arrays.copyOfRange(bytes, from, from + Node.COMPACT_NODE_INFO_BYTES))
    //                .map(Node::fromCompactNodeInfo)
    //                .collect(Collectors.toList());
    //    }

    private static byte[] packNodes(Collection<Node> nodes) {
        ByteBuffer buffer = ByteBuffer.allocate(nodes.size() * Node.COMPACT_NODE_INFO_BYTES);
        nodes.forEach(node -> buffer.put(node.toCompactNodeInfo()));
        return buffer.array();
    }

    public Collection<Node> getNodes() {
        return nodes;
    }

    @Override
    protected Map<String, Object> getResponseSpecificReturnValues() {
        return Map.of(KEY_NODES, ByteBuffer.wrap(packNodes(nodes)));
    }
}
