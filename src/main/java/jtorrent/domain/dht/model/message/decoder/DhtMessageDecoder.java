package jtorrent.domain.dht.model.message.decoder;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

import jtorrent.domain.common.util.bencode.BencodedMap;
import jtorrent.domain.dht.model.message.DhtMessage;
import jtorrent.domain.dht.model.message.MessageType;
import jtorrent.domain.dht.model.message.TransactionId;
import jtorrent.domain.dht.model.message.error.Error;
import jtorrent.domain.dht.model.message.query.AnnouncePeer;
import jtorrent.domain.dht.model.message.query.FindNode;
import jtorrent.domain.dht.model.message.query.GetPeers;
import jtorrent.domain.dht.model.message.query.Method;
import jtorrent.domain.dht.model.message.query.Ping;
import jtorrent.domain.dht.model.message.query.Query;
import jtorrent.domain.dht.model.message.response.AnnouncePeerResponse;
import jtorrent.domain.dht.model.message.response.FindNodeResponse;
import jtorrent.domain.dht.model.message.response.GetPeersResponse;
import jtorrent.domain.dht.model.message.response.PingResponse;
import jtorrent.domain.dht.model.message.response.Response;

public class DhtMessageDecoder {

    private final TransactionIdMethodProvider transactionIdMethodProvider;

    public DhtMessageDecoder(TransactionIdMethodProvider transactionIdMethodProvider) {
        this.transactionIdMethodProvider = requireNonNull(transactionIdMethodProvider);
    }

    public DhtMessage decode(byte[] data) throws DhtDecodingException {
        BencodedMap bencodedMap;

        try {
            bencodedMap = BencodedMap.decode(data);
        } catch (IOException e) {
            throw new DhtDecodingException("Failed to decode DHT message into map", e);
        }

        try {
            return decode(bencodedMap);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new DhtDecodingException("Failed to decode DHT message from map: " + bencodedMap, e);
        }
    }

    private DhtMessage decode(BencodedMap bencodedMap) throws DhtDecodingException {
        MessageType messageType = MessageType.fromValue(bencodedMap.getChar(DhtMessage.KEY_MESSAGE_TYPE));
        switch (messageType) {
        case QUERY:
            return decodeQuery(bencodedMap);
        case RESPONSE:
            return decodeResponse(bencodedMap);
        case ERROR:
            return Error.fromMap(bencodedMap);
        default:
            throw new AssertionError("Unknown message type: " + messageType);
        }
    }

    private Query decodeQuery(BencodedMap map) {
        Method method = Method.fromValue(map.getString(Query.KEY_METHOD_NAME));
        switch (method) {
        case PING:
            return Ping.fromMap(map);
        case FIND_NODE:
            return FindNode.fromMap(map);
        case GET_PEERS:
            return GetPeers.fromMap(map);
        case ANNOUNCE_PEER:
            return AnnouncePeer.fromMap(map);
        default:
            throw new AssertionError("Unknown method: " + method);
        }
    }

    private Response decodeResponse(BencodedMap bencodedMap) throws DhtDecodingException {
        byte[] transactionIdBytes = bencodedMap.getBytes(DhtMessage.KEY_TRANSACTION_ID).array();
        TransactionId transactionId = TransactionId.fromBytes(transactionIdBytes);
        Method method = transactionIdMethodProvider
                .getMethod(transactionId)
                .orElseThrow(() ->
                        new DhtDecodingException("No method associated with transaction ID: " + transactionId));

        switch (method) {
        case PING:
            return PingResponse.fromMap(bencodedMap);
        case FIND_NODE:
            return FindNodeResponse.fromMap(bencodedMap);
        case GET_PEERS:
            return GetPeersResponse.fromMap(bencodedMap);
        case ANNOUNCE_PEER:
            return AnnouncePeerResponse.fromMap(bencodedMap);
        default:
            throw new AssertionError("Unknown method: " + method);
        }
    }

    public interface TransactionIdMethodProvider {

        /**
         * Gets the associated {@link Method} for the given {@link TransactionId}.
         * The {@link Method} is used to determine how a {@link Response} should be decoded.
         *
         * @param transactionId the {@link TransactionId}
         * @return an {@link Optional} containing the {@link Method} associated with the given {@link TransactionId}
         * or {@link Optional#empty()} if no {@link Method} is associated with the given {@link TransactionId}
         */
        Optional<Method> getMethod(TransactionId transactionId);
    }
}
