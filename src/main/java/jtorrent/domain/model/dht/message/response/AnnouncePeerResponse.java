package jtorrent.domain.model.dht.message.response;

import java.util.Collections;
import java.util.Map;

import jtorrent.domain.model.dht.message.TransactionId;
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

    public static AnnouncePeerResponse fromUndefinedResponse(UndefinedResponse response) {
        BencodedMap returnValues = response.getReturnValues();
        NodeId id = new NodeId(returnValues.getBytes(KEY_ID).array());
        return new AnnouncePeerResponse(response.getTransactionId(), id);
    }

    @Override
    protected Map<String, Object> getResponseSpecificReturnValues() {
        return Collections.emptyMap();
    }
}
