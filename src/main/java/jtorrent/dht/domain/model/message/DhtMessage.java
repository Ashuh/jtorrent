package jtorrent.dht.domain.model.message;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import jtorrent.common.domain.util.bencode.BencodedMap;
import jtorrent.common.domain.util.bencode.BencodedObject;

public abstract class DhtMessage extends BencodedObject {

    public static final String KEY_TRANSACTION_ID = "t";
    public static final String KEY_MESSAGE_TYPE = "y";
    public static final String KEY_CLIENT_VERSION = "v";

    private final TransactionId transactionId;
    private final MessageType messageType;
    /**
     * A two character client identifier registered in BEP 20 followed by a two character version identifier.
     * This field is optional.
     */
    private final String clientVersion;

    protected DhtMessage(TransactionId transactionId, MessageType messageType, String clientVersion) {
        this.transactionId = requireNonNull(transactionId);
        this.messageType = requireNonNull(messageType);
        this.clientVersion = clientVersion;
    }

    protected DhtMessage(TransactionId transactionId, MessageType messageType) {
        this(transactionId, messageType, null);
    }

    protected DhtMessage(MessageType messageType, String clientVersion) {
        this(TransactionId.generateRandom(), messageType, clientVersion);
    }

    protected DhtMessage(MessageType messageType) {
        this(messageType, null);
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
                KEY_TRANSACTION_ID, ByteBuffer.wrap(transactionId.getBytes()),
                KEY_MESSAGE_TYPE, getMessageType().getValue()
        );
    }

    public abstract MessageType getMessageType();

    protected static TransactionId getTransactionIdFromMap(BencodedMap map) {
        return TransactionId.fromBytes(map.getBytes(KEY_TRANSACTION_ID).array());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DhtMessage that = (DhtMessage) o;
        return Objects.equals(transactionId, that.transactionId)
                && messageType == that.messageType
                && Objects.equals(clientVersion, that.clientVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, messageType, clientVersion);
    }

    @Override
    public String toString() {
        return new String(bencode(), StandardCharsets.UTF_8);
    }
}
