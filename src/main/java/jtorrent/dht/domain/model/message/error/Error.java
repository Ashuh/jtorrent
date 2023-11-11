package jtorrent.dht.domain.model.message.error;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.util.Objects;

import jtorrent.common.domain.util.bencode.BencodedList;
import jtorrent.common.domain.util.bencode.BencodedMap;
import jtorrent.dht.domain.model.message.DhtMessage;
import jtorrent.dht.domain.model.message.MessageType;
import jtorrent.dht.domain.model.message.TransactionId;

public class Error extends DhtMessage {

    public static final String KEY_LIST = "e";

    private final ErrorCode errorCode;
    private final String errorMessage;

    public Error(ErrorCode errorCode, String errorMessage) {
        super(MessageType.ERROR);
        this.errorCode = requireNonNull(errorCode);
        this.errorMessage = requireNonNull(errorMessage);
    }

    public Error(TransactionId transactionId, ErrorCode errorCode, String errorMessage) {
        super(transactionId, MessageType.ERROR);
        this.errorCode = requireNonNull(errorCode);
        this.errorMessage = requireNonNull(errorMessage);
    }

    public Error(TransactionId transactionId, String clientVersion, ErrorCode errorCode, String errorMessage) {
        super(transactionId, MessageType.ERROR, clientVersion);
        this.errorCode = requireNonNull(errorCode);
        this.errorMessage = requireNonNull(errorMessage);
    }

    public static Error fromMap(BencodedMap map) {
        TransactionId txId = getTransactionIdFromMap(map);
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedList list = map.getList(KEY_LIST);
        if (list.size() != 2) {
            throw new IllegalArgumentException("List size must be 2");
        }
        ErrorCode errorCode = ErrorCode.fromValue((list.getInt(0)));
        String errorMessage = list.getString(1);
        return new Error(txId, clientVersion, errorCode, errorMessage);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ERROR;
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
        Error error = (Error) o;
        return errorCode == error.errorCode && Objects.equals(errorMessage, error.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorCode, errorMessage);
    }
}
