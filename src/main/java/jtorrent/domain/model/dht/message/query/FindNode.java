package jtorrent.domain.model.dht.message.query;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.util.bencode.BencodedMap;

public class FindNode extends Query {

    protected static final String KEY_TARGET = "target";

    private final NodeId target;

    public FindNode(NodeId id, NodeId target) {
        super(Method.FIND_NODE, id);
        this.target = requireNonNull(target);
    }

    public FindNode(TransactionId transactionId, NodeId id, NodeId target) {
        super(transactionId, Method.FIND_NODE, id);
        this.target = requireNonNull(target);
    }

    public FindNode(TransactionId transactionId, String clientVersion, NodeId id, NodeId target) {
        super(transactionId, clientVersion, Method.FIND_NODE, id);
        this.target = requireNonNull(target);
    }

    public static FindNode fromMap(BencodedMap map) {
        TransactionId txId = TransactionId.fromBytes(map.getBytes(KEY_TRANSACTION_ID).array());
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedMap args = map.getMap(KEY_ARGS);
        NodeId id = new NodeId(args.getBytes(KEY_ID).array());
        NodeId target = new NodeId(args.getBytes(KEY_TARGET).array());
        return new FindNode(txId, clientVersion, id, target);
    }

    public NodeId getTarget() {
        return target;
    }

    @Override
    protected Map<String, Object> getQuerySpecificArgs() {
        return Map.of(KEY_TARGET, ByteBuffer.wrap(target.getBytes()));
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
        FindNode findNode = (FindNode) o;
        return Objects.equals(target, findNode.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), target);
    }
}
