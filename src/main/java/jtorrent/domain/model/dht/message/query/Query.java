package jtorrent.domain.model.dht.message.query;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jtorrent.domain.model.dht.message.DhtMessage;
import jtorrent.domain.model.dht.message.MessageType;
import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.node.NodeId;

public abstract class Query extends DhtMessage {

    public static final String KEY_METHOD_NAME = "q";
    public static final String KEY_ID = "id";
    public static final String KEY_ARGS = "a";

    /**
     * Querying node's id.
     */
    private final NodeId id;
    private final Method method;

    protected Query(Method method, NodeId id) {
        super(MessageType.QUERY);
        this.method = requireNonNull(method);
        this.id = requireNonNull(id);
    }

    protected Query(TransactionId transactionId, Method method, NodeId id) {
        super(transactionId, MessageType.QUERY);
        this.method = requireNonNull(method);
        this.id = requireNonNull(id);
    }

    protected Query(TransactionId transactionId, String clientVersion, Method method, NodeId id) {
        super(transactionId, MessageType.QUERY, clientVersion);
        this.method = requireNonNull(method);
        this.id = requireNonNull(id);
    }

    public NodeId getId() {
        return id;
    }

    @Override
    protected final Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(super.toMap());
        map.put(KEY_METHOD_NAME, method.getValue());
        map.put(KEY_ARGS, getArgs());
        return map;
    }

    @Override
    public final MessageType getMessageType() {
        return MessageType.QUERY;
    }

    public Method getMethod() {
        return method;
    }

    private Map<String, Object> getArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put(KEY_ID, ByteBuffer.wrap(id.getBytes()));
        args.putAll(getQuerySpecificArgs());
        return args;
    }

    protected abstract Map<String, Object> getQuerySpecificArgs();

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
        Query query = (Query) o;
        return Objects.equals(id, query.id) && method == query.method;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, method);
    }
}