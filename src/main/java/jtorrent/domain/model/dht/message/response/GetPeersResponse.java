package jtorrent.domain.model.dht.message.response;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.query.GetPeers;
import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.model.dht.node.NodeId;
import jtorrent.domain.model.peer.OutgoingPeer;
import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.util.bencode.BencodedMap;

/**
 * Represents a response to a {@link GetPeers} request.
 * The response contains a collection of {@link Peer Peers} if the queried node has peers for the requested info hash.
 * Otherwise, the response contains the K closest {@link Node Nodes} to the requested info hash.
 */
public class GetPeersResponse extends DefinedResponse {

    private static final String KEY_NODES = "nodes";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_VALUES = "values";

    private final String token;
    private final Collection<Peer> peers;
    private final Collection<Node> nodes;

    protected GetPeersResponse(NodeId id, String token, Collection<Peer> peers, Collection<Node> nodes) {
        super(id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    protected GetPeersResponse(TransactionId transactionId, NodeId id, String token, Collection<Peer> peers,
            Collection<Node> nodes) {
        super(transactionId, id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    protected GetPeersResponse(TransactionId transactionId, String clientVersion, NodeId id, String token,
            Collection<Peer> peers, Collection<Node> nodes) {
        super(transactionId, clientVersion, id);
        this.token = token;
        this.peers = peers;
        this.nodes = nodes;
    }

    public static GetPeersResponse fromUndefinedResponse(UndefinedResponse response) {
        BencodedMap returnValues = response.getReturnValues();
        NodeId id = new NodeId(returnValues.getBytes(KEY_ID).array());
        String token = returnValues.getString(KEY_TOKEN);
        Collection<Peer> peers = returnValues.getOptionalList(KEY_VALUES)
                .map(list -> list.stream()
                        .map(x -> (ByteBuffer) x)
                        .map(ByteBuffer::array)
                        .map(OutgoingPeer::fromCompactPeerInfo)
                        .collect(Collectors.toList())
                )
                .orElse(null);

        Collection<Node> nodes = returnValues.getOptionalBytes(KEY_NODES)
                .map(ByteBuffer::array)
                .map(Node::multipleFromCompactNodeInfo)
                .orElse(null);

        if (peers == null && nodes == null) {
            throw new IllegalArgumentException("Either peers or nodes must be present");
        }

        return new GetPeersResponse(response.getTransactionId(), id, token, peers, nodes);
    }

    public String getToken() {
        return token;
    }

    public boolean hasPeers() {
        return peers != null;
    }

    public Optional<Collection<Peer>> getPeers() {
        return Optional.ofNullable(peers);
    }

    public Optional<Collection<Node>> getNodes() {
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
            returnValues.put(KEY_NODES, ByteBuffer.wrap(Node.multipleToCompactNodeInfo(nodes)));
        }

        return returnValues;
    }
}