package jtorrent.domain.model.dht.message.error;

import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.Objects;

import jtorrent.domain.model.dht.message.DhtDecodingException;
import jtorrent.domain.model.dht.message.DhtMessage;
import jtorrent.domain.model.dht.message.MessageType;
import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.util.bencode.BencodedList;
import jtorrent.domain.util.bencode.BencodedMap;

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

    public static Error fromMap(BencodedMap map) throws DhtDecodingException {
        try {
            TransactionId txId = getTransactionIdFromMap(map);
            String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
            BencodedList list = map.getList(KEY_LIST);
            if (list.size() != 2) {
                throw new DhtDecodingException("Failed to decode Error: list size is not 2");
            }
            ErrorCode errorCode = ErrorCode.fromValue((list.getInt(0)));
            String errorMessage = list.getString(1);
            return new Error(txId, clientVersion, errorCode, errorMessage);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new DhtDecodingException("Failed to decode Error", e);
        }
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
