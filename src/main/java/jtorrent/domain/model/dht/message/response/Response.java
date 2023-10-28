package jtorrent.domain.model.dht.message.response;

import java.util.HashMap;
import java.util.Map;

import jtorrent.domain.model.dht.message.DhtMessage;
import jtorrent.domain.model.dht.message.MessageType;
import jtorrent.domain.model.dht.message.TransactionId;

// TODO: merge with DefinedResponse
public abstract class Response extends DhtMessage {

    protected static final String KEY_RETURN_VALUES = "r";

    protected Response() {
        super(MessageType.RESPONSE);
    }

    protected Response(TransactionId transactionId) {
        super(transactionId, MessageType.RESPONSE);
    }

    protected Response(TransactionId transactionId, String clientVersion) {
        super(transactionId, MessageType.RESPONSE, clientVersion);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(super.toMap());
        map.put(KEY_RETURN_VALUES, getReturnValues());
        return map;
    }

    protected abstract Map<String, Object> getReturnValues();

    @Override
    public MessageType getMessageType() {
        return MessageType.RESPONSE;
    }
}
