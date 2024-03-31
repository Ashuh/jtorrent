package jtorrent.domain.dht.model.message.response;

import java.util.Collections;
import java.util.Map;

import jtorrent.domain.common.util.bencode.BencodedMap;
import jtorrent.domain.dht.model.message.DhtMessage;
import jtorrent.domain.dht.model.message.TransactionId;
import jtorrent.domain.dht.model.message.query.Method;
import jtorrent.domain.dht.model.node.NodeId;

public class PingResponse extends Response {

    public PingResponse(NodeId id) {
        super(id);
    }

    public PingResponse(TransactionId transactionId, NodeId id) {
        super(transactionId, id);
    }

    public PingResponse(TransactionId transactionId, String clientVersion, NodeId id) {
        super(transactionId, clientVersion, id);
    }

    public static PingResponse fromMap(BencodedMap map) {
        TransactionId txId = DhtMessage.getTransactionIdFromMap(map);
        String clientVersion = map.getOptionalString(DhtMessage.KEY_CLIENT_VERSION).orElse(null);
        BencodedMap returnValues = getReturnValuesFromMap(map);
        NodeId nodeId = getNodeIdFromMap(returnValues);
        return new PingResponse(txId, clientVersion, nodeId);
    }

    @Override
    protected Map<String, Object> getResponseSpecificReturnValues() {
        return Collections.emptyMap();
    }

    @Override
    public Method getMethod() {
        return Method.PING;
    }
}
