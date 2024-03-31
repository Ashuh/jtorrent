package jtorrent.domain.dht.handler.lookup;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.model.message.response.FindNodeResponse;
import jtorrent.domain.dht.model.node.NodeId;

public class FindNodeLookup extends IterativeLookup<FindNodeResponse, NodeId, Collection<Node>> {

    private static final String NAME = "find node lookup";

    @Override
    protected Collection<Node> getResult() {
        return getClosestNodesSeen();
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected Collection<Node> getNodesFromResponse(FindNodeResponse response) {
        return response.getNodes().stream()
                .map(Node::withContactInfo)
                .collect(Collectors.toList());
    }

    @Override
    protected CompletableFuture<FindNodeResponse> doQuery(Node node, NodeId target) {
        return node.findNode(target);
    }
}
