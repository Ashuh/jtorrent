package jtorrent.domain.model.dht.message.response;

import static jtorrent.domain.util.ValidationUtil.requireNonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jtorrent.domain.model.dht.message.DhtDecodingException;
import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.query.GetPeers;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.node.NodeContactInfo;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.model.peer.PeerContactInfo;
import jtorrent.domain.util.bencode.BencodedMap;

/**
 * Represents a response to a {@link GetPeers} request.
 * The response contains a collection of {@link PeerContactInfo}s if the queried node has peers for the requested
 * info hash.
 * Otherwise, the response contains the K closest {@link NodeContactInfo Nodes} to the requested info hash.
 */
public class GetPeersResponse extends DefinedResponse {

    private static final String KEY_NODES = "nodes";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_VALUES = "values";

    private final byte[] token;
    private final Collection<PeerContactInfo> peers;
    private final Collection<NodeContactInfo> nodes;

    private GetPeersResponse(NodeId id, byte[] token, Collection<PeerContactInfo> peers,
            Collection<NodeContactInfo> nodes) {
        super(id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    private GetPeersResponse(TransactionId transactionId, NodeId id, byte[] token, Collection<PeerContactInfo> peers,
            Collection<NodeContactInfo> nodes) {
        super(transactionId, id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    private GetPeersResponse(TransactionId transactionId, String clientVersion, NodeId id, byte[] token,
            Collection<PeerContactInfo> peers, Collection<NodeContactInfo> nodes) {
        super(transactionId, clientVersion, id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    public static GetPeersResponse withPeers(NodeId id, byte[] token, Collection<PeerContactInfo> peers) {
        return new GetPeersResponse(id, token, requireNonNull(peers), null);
    }

    public static GetPeersResponse withNodes(NodeId id, byte[] token, Collection<NodeContactInfo> nodes) {
        return new GetPeersResponse(id, token, null, requireNonNull(nodes));
    }

    public static GetPeersResponse fromMap(BencodedMap map) throws DhtDecodingException {
        try {
            TransactionId txId = getTransactionIdFromMap(map);
            String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
            BencodedMap returnValues = getReturnValuesFromMap(map);
            NodeId nodeId = getNodeIdFromMap(returnValues);
            byte[] token = returnValues.getBytes(KEY_TOKEN).array();
            Collection<PeerContactInfo> peers = getPeersFromMap(returnValues).orElse(null);
            Collection<NodeContactInfo> nodes = getNodesFromMap(returnValues).orElse(null);

            if (peers == null && nodes == null) {
                throw new DhtDecodingException("Failed to decode GetPeersResponse: Both peers and nodes are null");
            }

            return new GetPeersResponse(txId, clientVersion, nodeId, token, peers, nodes);

        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new DhtDecodingException("Failed to decode GetPeersResponse", e);
        }
    }

    private static Optional<Collection<PeerContactInfo>> getPeersFromMap(BencodedMap returnValues) {
        return returnValues.getOptionalList(KEY_VALUES)
                .map(list -> list.stream()
                        .map(x -> (ByteBuffer) x)
                        .map(ByteBuffer::array)
                        .map(PeerContactInfo::fromCompactPeerInfo)
                        .collect(Collectors.toList())
                );
    }

    private static Optional<Collection<NodeContactInfo>> getNodesFromMap(BencodedMap returnValues) {
        return returnValues.getOptionalBytes(KEY_NODES)
                .map(ByteBuffer::array)
                .map(NodeContactInfo::multipleFromCompactNodeInfo);
    }

    public byte[] getToken() {
        return token;
    }

    public boolean hasPeers() {
        return peers != null;
    }

    public Optional<Collection<PeerContactInfo>> getPeers() {
        return Optional.ofNullable(peers);
    }

    public Optional<Collection<NodeContactInfo>> getNodes() {
        return Optional.ofNullable(nodes);
    }

    @Override
    protected Map<String, Object> getResponseSpecificReturnValues() {
        Map<String, Object> returnValues = new HashMap<>();
        returnValues.put(KEY_TOKEN, token);

        if (peers != null) {
            returnValues.put(KEY_VALUES, peers.stream()
                    .map(PeerContactInfo::toCompactPeerInfo)
                    .map(ByteBuffer::wrap)
                    .collect(Collectors.toList())
            );
        }
        if (nodes != null) {
            returnValues.put(KEY_NODES, ByteBuffer.wrap(NodeContactInfo.multipleToCompactNodeInfo(nodes)));
        }

        return returnValues;
    }

    @Override
    public Method getMethod() {
        return Method.GET_PEERS;
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
        GetPeersResponse that = (GetPeersResponse) o;
        return Arrays.equals(token, that.token)
                && Objects.equals(peers, that.peers)
                && Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), peers, nodes);
        result = 31 * result + Arrays.hashCode(token);
        return result;
    }
}
