package jtorrent.domain.model.dht.message.query;

import static jtorrent.domain.util.ValidationUtil.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import jtorrent.domain.model.dht.message.DhtDecodingException;
import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.util.Sha1Hash;
import jtorrent.domain.util.bencode.BencodedMap;

public class GetPeers extends Query {

    private static final String KEY_INFO_HASH = "info_hash";

    private final Sha1Hash infoHash;

    public GetPeers(NodeId id, Sha1Hash infoHash) {
        super(Method.GET_PEERS, id);
        this.infoHash = requireNonNull(infoHash);
    }

    public GetPeers(TransactionId transactionId, NodeId id, Sha1Hash infoHash) {
        super(transactionId, Method.GET_PEERS, id);
        this.infoHash = requireNonNull(infoHash);
    }

    public GetPeers(TransactionId transactionId, String clientVersion, NodeId id, Sha1Hash infoHash) {
        super(transactionId, clientVersion, Method.GET_PEERS, id);
        this.infoHash = requireNonNull(infoHash);
    }

    public static GetPeers fromMap(BencodedMap map) throws DhtDecodingException {
        try {
            TransactionId txId = getTransactionIdFromMap(map);
            String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
            BencodedMap args = getArgsFromMap(map);
            NodeId id = getNodeIdFromMap(args);
            Sha1Hash infoHash = getInfoHashFromMap(args);
            return new GetPeers(txId, clientVersion, id, infoHash);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new DhtDecodingException("Failed to decode GetPeers", e);
        }
    }

    private static Sha1Hash getInfoHashFromMap(BencodedMap args) {
        return new Sha1Hash(args.getBytes(KEY_INFO_HASH).array());
    }

    public Sha1Hash getInfoHash() {
        return infoHash;
    }

    protected Map<String, Object> getQuerySpecificArgs() {
        return Map.of(KEY_INFO_HASH, ByteBuffer.wrap(infoHash.getBytes()));
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
        GetPeers getPeers = (GetPeers) o;
        return Objects.equals(infoHash, getPeers.infoHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), infoHash);
    }
}
