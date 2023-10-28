package jtorrent.domain.manager.dht.lookup;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import jtorrent.domain.model.dht.message.response.GetPeersResponse;
import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.model.peer.PeerContactInfo;
import jtorrent.domain.util.Sha1Hash;

public class GetPeersLookup extends IterativeLookup<GetPeersResponse, Sha1Hash, GetPeersLookup.Result> {

    private static final Logger LOGGER = System.getLogger(GetPeersLookup.class.getName());
    private static final String NAME = "get peers";

    private final Set<PeerContactInfo> peers = new HashSet<>();
    private final Map<Node, byte[]> nodeContactInfoToToken = new HashMap<>();

    @Override
    protected Result getResult() {
        Map<Node, byte[]> closestNodeToContactInfo = getClosestNodesSeen().stream()
                .collect(Collectors.toMap(Function.identity(), nodeContactInfoToToken::get));
        return new Result(peers, closestNodeToContactInfo);
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected Collection<Node> getNodesFromResponse(GetPeersResponse response) {
        return response.getNodes()
                .orElse(Collections.emptyList())
                .stream()
                .map(Node::withContactInfo)
                .collect(Collectors.toList());
    }

    @Override
    protected CompletableFuture<GetPeersResponse> doQuery(Node node, Sha1Hash target) {
        return node.getPeers(target)
                .whenComplete((response, throwable) -> {
                    if (response != null) {
                        response.getPeers().ifPresent(peers::addAll);
                        nodeContactInfoToToken.put(node, response.getToken());
                        // log num peers
                        LOGGER.log(Level.WARNING, "[DHT] Got {0} peers from {1}",
                                response.getPeers().orElse(Collections.emptyList()).size(), node);
                    } else {
                        LOGGER.log(Level.WARNING, "Failed to get peers from node " + node, throwable);
                    }
                });
    }

    public static class Result {

        private final Collection<PeerContactInfo> peers = new HashSet<>();
        private final Map<Node, byte[]> nodeToToken = new HashMap<>();

        public Result(Collection<PeerContactInfo> peers, Map<Node, byte[]> nodeToToken) {
            this.peers.addAll(peers);
            this.nodeToToken.putAll(nodeToToken);
        }

        public Collection<PeerContactInfo> getPeers() {
            return peers;
        }

        public Map<Node, byte[]> getNodeToToken() {
            return nodeToToken;
        }
    }
}
