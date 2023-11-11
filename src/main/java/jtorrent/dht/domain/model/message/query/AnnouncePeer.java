package jtorrent.dht.domain.model.message.query;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.common.domain.util.bencode.BencodedMap;
import jtorrent.dht.domain.model.message.TransactionId;
import jtorrent.dht.domain.model.node.NodeId;

public class AnnouncePeer extends Query {

    private static final String KEY_PORT = "port";
    private static final String KEY_INFO_HASH = "info_hash";
    private static final String KEY_TOKEN = "token";

    private final int port;
    private final Sha1Hash infoHash;
    private final byte[] token;

    public AnnouncePeer(NodeId id, Sha1Hash infoHash, int port, byte[] token) {
        super(Method.ANNOUNCE_PEER, id);
        this.infoHash = requireNonNull(infoHash);
        this.port = port;
        this.token = requireNonNull(token);
    }

    public AnnouncePeer(TransactionId transactionId, NodeId id, Sha1Hash infoHash, int port, byte[] token) {
        super(transactionId, Method.ANNOUNCE_PEER, id);
        this.infoHash = requireNonNull(infoHash);
        this.port = port;
        this.token = requireNonNull(token);
    }

    public AnnouncePeer(TransactionId transactionId, String clientVersion, NodeId id, Sha1Hash infoHash, int port,
            byte[] token) {
        super(transactionId, clientVersion, Method.ANNOUNCE_PEER, id);
        this.infoHash = requireNonNull(infoHash);
        this.port = port;
        this.token = requireNonNull(token);
    }

    public static AnnouncePeer fromMap(BencodedMap map) {
        TransactionId txId = getTransactionIdFromMap(map);
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedMap args = getArgsFromMap(map);
        NodeId id = getNodeIdFromMap(args);
        Sha1Hash infoHash = getInfoHashFromMap(args);
        int port = args.getInt(KEY_PORT);
        byte[] token = args.getBytes(KEY_TOKEN).array();
        return new AnnouncePeer(txId, clientVersion, id, infoHash, port, token);
    }

    private static Sha1Hash getInfoHashFromMap(BencodedMap args) {
        return new Sha1Hash(args.getBytes(KEY_INFO_HASH).array());
    }

    public Sha1Hash getInfoHash() {
        return infoHash;
    }

    public int getPort() {
        return port;
    }

    public byte[] getToken() {
        return token;
    }

    @Override
    protected Map<String, Object> getQuerySpecificArgs() {
        return Map.of(
                KEY_INFO_HASH, ByteBuffer.wrap(infoHash.getBytes()),
                KEY_PORT, port,
                KEY_TOKEN, token
        );
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
        AnnouncePeer that = (AnnouncePeer) o;
        return port == that.port
                && Objects.equals(infoHash, that.infoHash)
                && Arrays.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), port, infoHash);
        result = 31 * result + Arrays.hashCode(token);
        return result;
    }
}
