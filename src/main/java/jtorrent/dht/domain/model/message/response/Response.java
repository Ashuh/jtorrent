package jtorrent.dht.domain.model.message.response;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jtorrent.dht.domain.model.message.DhtMessage;
import jtorrent.dht.domain.model.message.MessageType;
import jtorrent.dht.domain.model.message.TransactionId;
import jtorrent.dht.domain.model.message.query.Method;
import jtorrent.dht.domain.model.message.query.Query;
import jtorrent.dht.domain.model.node.NodeId;
import jtorrent.common.domain.util.bencode.BencodedMap;

public abstract class Response extends DhtMessage {

    protected static final String KEY_ID = "id";
    protected static final String KEY_RETURN_VALUES = "r";

    /**
     * The ID of the queried node.
     */
    private final NodeId id;

    protected Response(NodeId id) {
        super(MessageType.RESPONSE);
        this.id = requireNonNull(id);
    }

    protected Response(TransactionId transactionId, NodeId id) {
        super(transactionId, MessageType.RESPONSE);
        this.id = id;
    }

    protected Response(TransactionId transactionId, String clientVersion, NodeId id) {
        super(transactionId, MessageType.RESPONSE, clientVersion);
        this.id = id;
    }

    protected static NodeId getNodeIdFromMap(BencodedMap map) {
        return new NodeId(map.getBytes(KEY_ID).array());
    }

    protected static BencodedMap getReturnValuesFromMap(BencodedMap map) {
        return map.getMap(KEY_RETURN_VALUES);
    }

    public NodeId getId() {
        return id;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(super.toMap());
        map.put(KEY_RETURN_VALUES, getReturnValues());
        return map;
    }

    private Map<String, Object> getReturnValues() {
        Map<String, Object> returnValues = new HashMap<>();
        returnValues.put(KEY_ID, id);
        returnValues.putAll(getResponseSpecificReturnValues());
        return returnValues;
    }

    protected abstract Map<String, Object> getResponseSpecificReturnValues();

    @Override
    public MessageType getMessageType() {
        return MessageType.RESPONSE;
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
        Response response = (Response) o;
        return Objects.equals(id, response.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    /**
     * Gets the {@link Method} of the {@link Query} that this {@link Response} is a response to.
     */
    public abstract Method getMethod();
}
