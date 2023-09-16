package jtorrent.domain.model.dht.message.query;

import java.util.Collections;
import java.util.Map;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.util.bencode.BencodedMap;

public class Ping extends Query {

    public Ping(NodeId id) {
        super(Method.PING, id);
    }

    public Ping(TransactionId transactionId, NodeId id) {
        super(transactionId, Method.PING, id);
    }

    public Ping(TransactionId transactionId, String clientVersion, NodeId id) {
        super(transactionId, clientVersion, Method.PING, id);
    }

    public static Ping fromMap(BencodedMap map) {
        TransactionId txId = TransactionId.fromBytes(map.getBytes(KEY_TRANSACTION_ID).array());
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedMap args = map.getMap(KEY_ARGS);
        NodeId id = new NodeId(args.getBytes(KEY_ID).array());
        return new Ping(txId, clientVersion, id);
    }

    @Override
    protected Map<String, Object> getQuerySpecificArgs() {
        return Collections.emptyMap();
    }
}
