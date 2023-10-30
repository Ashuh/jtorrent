package jtorrent.domain.model.dht.message.query;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.decoder.DhtDecodingException;
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

    public static Ping fromMap(BencodedMap map) throws DhtDecodingException {
        try {
            TransactionId txId = getTransactionIdFromMap(map);
            String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
            BencodedMap args = getArgsFromMap(map);
            NodeId id = getNodeIdFromMap(args);
            return new Ping(txId, clientVersion, id);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new DhtDecodingException("Failed to decode Ping", e);
        }
    }

    @Override
    protected Map<String, Object> getQuerySpecificArgs() {
        return Collections.emptyMap();
    }
}
