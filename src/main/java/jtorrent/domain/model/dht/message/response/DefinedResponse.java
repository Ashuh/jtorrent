package jtorrent.domain.model.dht.message.response;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.node.NodeId;

public abstract class DefinedResponse extends Response {

    protected static final String KEY_ID = "id";

    /**
     * The ID of the queried node.
     */
    private final NodeId id;

    protected DefinedResponse(NodeId id) {
        super();
        this.id = id;
    }

    protected DefinedResponse(TransactionId transactionId, NodeId id) {
        super(transactionId);
        this.id = id;
    }

    protected DefinedResponse(TransactionId transactionId, String clientVersion, NodeId id) {
        super(transactionId, clientVersion);
        this.id = id;
    }

    public NodeId getId() {
        return id;
    }

    @Override
    protected final Map<String, Object> getReturnValues() {
        Map<String, Object> returnValues = new HashMap<>();
        returnValues.put(KEY_ID, id);
        returnValues.putAll(getResponseSpecificReturnValues());
        return returnValues;
    }

    protected abstract Map<String, Object> getResponseSpecificReturnValues();

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
        DefinedResponse that = (DefinedResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
