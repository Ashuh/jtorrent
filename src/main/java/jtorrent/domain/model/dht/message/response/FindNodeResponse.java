package jtorrent.domain.model.dht.message.response;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.node.NodeContactInfo;
import jtorrent.domain.model.dht.node.NodeId;

public class FindNodeResponse extends DefinedResponse {

    private static final String KEY_NODES = "nodes";

    private final Collection<NodeContactInfo> nodes;

    public FindNodeResponse(NodeId id, Collection<NodeContactInfo> nodes) {
        super(id);
        this.nodes = nodes;
    }

    public FindNodeResponse(TransactionId transactionId, NodeId id, Collection<NodeContactInfo> nodes) {
        super(transactionId, id);
        this.nodes = nodes;
    }

    public FindNodeResponse(TransactionId transactionId, String clientVersion, NodeId id,
            Collection<NodeContactInfo> nodes) {
        super(transactionId, clientVersion, id);
        this.nodes = nodes;
    }

    public static FindNodeResponse fromUnknownResponse(UndefinedResponse response) {
        TransactionId transactionId = response.getTransactionId();

        byte[] nodeIdBytes = response.getReturnValues().getBytes(KEY_ID).array();
        NodeId nodeId = new NodeId(nodeIdBytes);

        byte[] nodesBytes = response.getReturnValues().getBytes(KEY_NODES).array();
        Collection<NodeContactInfo> nodes = NodeContactInfo.multipleFromCompactNodeInfo(nodesBytes);

        return new FindNodeResponse(transactionId, nodeId, nodes);
    }

    private static byte[] packNodes(Collection<NodeContactInfo> nodes) {
        ByteBuffer buffer = ByteBuffer.allocate(nodes.size() * NodeContactInfo.COMPACT_NODE_INFO_BYTES);
        nodes.forEach(node -> buffer.put(node.toCompactNodeInfo()));
        return buffer.array();
    }

    public Collection<NodeContactInfo> getNodes() {
        return nodes;
    }

    @Override
    protected Map<String, Object> getResponseSpecificReturnValues() {
        return Map.of(KEY_NODES, ByteBuffer.wrap(packNodes(nodes)));
    }
}
