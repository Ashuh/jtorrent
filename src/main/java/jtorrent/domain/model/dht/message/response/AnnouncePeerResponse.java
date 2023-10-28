package jtorrent.domain.model.dht.message.response;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import jtorrent.domain.model.dht.message.DhtDecodingException;
import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.util.bencode.BencodedMap;

public class AnnouncePeerResponse extends DefinedResponse {

    protected AnnouncePeerResponse(NodeId id) {
        super(id);
    }

    protected AnnouncePeerResponse(TransactionId transactionId, NodeId id) {
        super(transactionId, id);
    }

    protected AnnouncePeerResponse(TransactionId transactionId, String clientVersion, NodeId id) {
        super(transactionId, clientVersion, id);
    }

    public static AnnouncePeerResponse fromMap(BencodedMap map) throws DhtDecodingException {
        try {
            TransactionId txId = getTransactionIdFromMap(map);
            String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
            BencodedMap returnValues = getReturnValuesFromMap(map);
            NodeId nodeId = getNodeIdFromMap(returnValues);
            return new AnnouncePeerResponse(txId, clientVersion, nodeId);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new DhtDecodingException("Failed to decode AnnouncePeerResponse", e);
        }
    }

    @Override
    protected Map<String, Object> getResponseSpecificReturnValues() {
        return Collections.emptyMap();
    }

    @Override
    public Method getMethod() {
        return Method.ANNOUNCE_PEER;
    }
}
