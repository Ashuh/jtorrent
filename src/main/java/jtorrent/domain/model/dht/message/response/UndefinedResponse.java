package jtorrent.domain.model.dht.message.response;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.util.bencode.BencodedMap;

public class UndefinedResponse extends Response {

    private final BencodedMap returnValues;

    public UndefinedResponse(BencodedMap returnValues) {
        super();
        this.returnValues = requireNonNull(returnValues);
    }

    public UndefinedResponse(TransactionId transactionId, BencodedMap returnValues) {
        super(transactionId);
        this.returnValues = requireNonNull(returnValues);
    }

    public UndefinedResponse(TransactionId transactionId, String clientVersion, BencodedMap returnValues) {
        super(transactionId, clientVersion);
        this.returnValues = requireNonNull(returnValues);
    }

    public static UndefinedResponse fromMap(BencodedMap map) {
        TransactionId txId = TransactionId.fromBytes(map.getBytes(KEY_TRANSACTION_ID).array());
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedMap returnValues = map.getMap(KEY_RETURN_VALUES);
        return new UndefinedResponse(txId, clientVersion, returnValues);
    }

    @Override
    protected final Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(super.toMap());
        map.put(KEY_RETURN_VALUES, returnValues);
        return map;
    }

    @Override
    protected BencodedMap getReturnValues() {
        return returnValues;
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
        UndefinedResponse that = (UndefinedResponse) o;
        return Objects.equals(returnValues, that.returnValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), returnValues);
    }
}
