package jtorrent.domain.model.dht.message.response;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.query.GetPeers;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.node.NodeContactInfo;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.model.peer.OutgoingPeer;
import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.util.bencode.BencodedMap;

/**
 * Represents a response to a {@link GetPeers} request.
 * The response contains a collection of {@link Peer Peers} if the queried node has peers for the requested info hash.
 * Otherwise, the response contains the K closest {@link NodeContactInfo Nodes} to the requested info hash.
 */
public class GetPeersResponse extends DefinedResponse {

    private static final String KEY_NODES = "nodes";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_VALUES = "values";

    private final byte[] token;
    private final Collection<Peer> peers;
    private final Collection<NodeContactInfo> nodes;

    protected GetPeersResponse(NodeId id, byte[] token, Collection<Peer> peers, Collection<NodeContactInfo> nodes) {
        super(id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    protected GetPeersResponse(TransactionId transactionId, NodeId id, byte[] token, Collection<Peer> peers,
            Collection<NodeContactInfo> nodes) {
        super(transactionId, id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    protected GetPeersResponse(TransactionId transactionId, String clientVersion, NodeId id, byte[] token,
            Collection<Peer> peers, Collection<NodeContactInfo> nodes) {
        super(transactionId, clientVersion, id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    public static GetPeersResponse fromMap(BencodedMap map) {
        TransactionId txId = TransactionId.fromBytes(map.getBytes(KEY_TRANSACTION_ID).array());
        String clientVersion = map.getOptionalString(KEY_CLIENT_VERSION).orElse(null);
        BencodedMap returnValues = map.getMap(KEY_RETURN_VALUES);

        NodeId nodeId = new NodeId(returnValues.getBytes(KEY_ID).array());
        byte[] token = returnValues.getBytes(KEY_TOKEN).array();

        Collection<Peer> peers = returnValues.getOptionalList(KEY_VALUES)
                .map(list -> list.stream()
                        .map(x -> (ByteBuffer) x)
                        .map(ByteBuffer::array)
                        .map(OutgoingPeer::fromCompactPeerInfo)
                        .collect(Collectors.toList())
                )
                .orElse(null);

        Collection<NodeContactInfo> nodes = returnValues.getOptionalBytes(KEY_NODES)
                .map(ByteBuffer::array)
                .map(NodeContactInfo::multipleFromCompactNodeInfo)
                .orElse(null);

        if (peers == null && nodes == null) {
            throw new IllegalArgumentException("Either peers or nodes must be present");
        }

        return new GetPeersResponse(txId, clientVersion, nodeId, token, peers, nodes);
    }

    public byte[] getToken() {
        return token;
    }

    public boolean hasPeers() {
        return peers != null;
    }

    public Optional<Collection<Peer>> getPeers() {
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
                    .map(Peer::toCompactPeerInfo)
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
}
