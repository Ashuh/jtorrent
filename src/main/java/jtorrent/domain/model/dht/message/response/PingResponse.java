package jtorrent.domain.model.dht.message.response;

import java.util.Collections;
import java.util.Map;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.util.bencode.BencodedMap;

public class PingResponse extends DefinedResponse {

    protected PingResponse(NodeId id) {
        super(id);
    }

    protected PingResponse(TransactionId transactionId, NodeId id) {
        super(transactionId, id);
    }

    protected PingResponse(TransactionId transactionId, String clientVersion, NodeId id) {
        super(transactionId, clientVersion, id);
    }

    public static PingResponse fromMap(BencodedMap map) {
        TransactionId txId = TransactionId.fromBytes(map.getBytes(KEY_TRANSACTION_ID).array());
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedMap returnValues = map.getMap(KEY_RETURN_VALUES);
        NodeId nodeId = new NodeId(returnValues.getBytes(KEY_ID).array());
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
