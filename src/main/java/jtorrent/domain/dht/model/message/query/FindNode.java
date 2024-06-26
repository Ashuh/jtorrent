package jtorrent.domain.dht.model.message.query;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import jtorrent.domain.common.util.Bit160Value;
import jtorrent.domain.common.util.bencode.BencodedMap;
import jtorrent.domain.dht.model.message.TransactionId;
import jtorrent.domain.dht.model.node.NodeId;

public class FindNode extends Query {

    protected static final String KEY_TARGET = "target";

    private final Bit160Value target;

    public FindNode(NodeId id, Bit160Value target) {
        super(Method.FIND_NODE, id);
        this.target = requireNonNull(target);
    }

    public FindNode(TransactionId transactionId, NodeId id, Bit160Value target) {
        super(transactionId, Method.FIND_NODE, id);
        this.target = requireNonNull(target);
    }

    public FindNode(TransactionId transactionId, String clientVersion, NodeId id, Bit160Value target) {
        super(transactionId, clientVersion, Method.FIND_NODE, id);
        this.target = requireNonNull(target);
    }

    public static FindNode fromMap(BencodedMap map) {
        TransactionId txId = getTransactionIdFromMap(map);
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedMap args = getArgsFromMap(map);
        NodeId id = getNodeIdFromMap(args);
        NodeId target = getTargetFromMap(args);
        return new FindNode(txId, clientVersion, id, target);
    }

    protected static NodeId getTargetFromMap(BencodedMap args) {
        return new NodeId(args.getBytes(KEY_TARGET).array());
    }

    public Bit160Value getTarget() {
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
