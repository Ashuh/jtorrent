package jtorrent.domain.model.dht.message.response;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.node.NodeContactInfo;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.util.bencode.BencodedMap;

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

    public static FindNodeResponse fromMap(BencodedMap map) {
        TransactionId txId = getTransactionIdFromMap(map);
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedMap returnValues = getReturnValuesFromMap(map);
        NodeId nodeId = getNodeIdFromMap(returnValues);
        Collection<NodeContactInfo> nodes = getNodesFromMap(returnValues);
        return new FindNodeResponse(txId, clientVersion, nodeId, nodes);
    }

    private static Collection<NodeContactInfo> getNodesFromMap(BencodedMap returnValues) {
        return NodeContactInfo.multipleFromCompactNodeInfo(returnValues.getBytes(KEY_NODES).array());
    }

    public Collection<NodeContactInfo> getNodes() {
        return nodes;
    }

    @Override
    protected Map<String, Object> getResponseSpecificReturnValues() {
        return Map.of(KEY_NODES, ByteBuffer.wrap(packNodes(nodes)));
    }

    private static byte[] packNodes(Collection<NodeContactInfo> nodes) {
        ByteBuffer buffer = ByteBuffer.allocate(nodes.size() * NodeContactInfo.COMPACT_NODE_INFO_BYTES);
        nodes.forEach(node -> buffer.put(node.toCompactNodeInfo()));
        return buffer.array();
    }

    @Override
    public Method getMethod() {
        return Method.FIND_NODE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        FindNodeResponse that = (FindNodeResponse) o;
        return Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodes);
    }
}
