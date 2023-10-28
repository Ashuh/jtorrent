package jtorrent.domain.model.dht.message;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

import jtorrent.domain.model.dht.message.error.Error;
import jtorrent.domain.model.dht.message.query.AnnouncePeer;
import jtorrent.domain.model.dht.message.query.FindNode;
import jtorrent.domain.model.dht.message.query.GetPeers;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.message.query.Ping;
import jtorrent.domain.model.dht.message.query.Query;
import jtorrent.domain.model.dht.message.response.AnnouncePeerResponse;
import jtorrent.domain.model.dht.message.response.FindNodeResponse;
import jtorrent.domain.model.dht.message.response.GetPeersResponse;
import jtorrent.domain.model.dht.message.response.PingResponse;
import jtorrent.domain.model.dht.message.response.Response;
import jtorrent.domain.util.bencode.BencodedMap;

public class DhtMessageDecoder {

    private final TransactionIdMethodProvider transactionIdMethodProvider;

    public DhtMessageDecoder(TransactionIdMethodProvider transactionIdMethodProvider) {
        this.transactionIdMethodProvider = requireNonNull(transactionIdMethodProvider);
    }

    public DhtMessage decode(byte[] data) throws DhtDecodingException {
        try {
            BencodedMap bencodedMap = BencodedMap.decode(data);
            return decode(bencodedMap);
        } catch (IOException e) {
            throw new DhtDecodingException(e);
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

    private Query decodeQuery(BencodedMap map) throws DhtDecodingException {
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
        try {
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
        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new DhtDecodingException("Failed to decode Response", e);
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
