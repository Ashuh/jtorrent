package jtorrent.domain.dht.model.message.query;

import java.util.Collections;
import java.util.Map;

import jtorrent.domain.common.util.bencode.BencodedMap;
import jtorrent.domain.dht.model.message.DhtMessage;
import jtorrent.domain.dht.model.message.TransactionId;
import jtorrent.domain.dht.model.node.NodeId;

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
        TransactionId txId = DhtMessage.getTransactionIdFromMap(map);
        String clientVersion = map.getOptionalString(DhtMessage.KEY_CLIENT_VERSION).orElse(null);
        BencodedMap args = getArgsFromMap(map);
        NodeId id = getNodeIdFromMap(args);
        return new Ping(txId, clientVersion, id);
    }

    @Override
    protected Map<String, Object> getQuerySpecificArgs() {
        return Collections.emptyMap();
    }
}
