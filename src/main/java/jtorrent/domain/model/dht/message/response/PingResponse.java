package jtorrent.domain.model.dht.message.response;

import java.util.Collections;
import java.util.Map;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.util.bencode.BencodedMap;

public class PingResponse extends DefinedResponse {

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
        TransactionId txId = getTransactionIdFromMap(map);
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
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
