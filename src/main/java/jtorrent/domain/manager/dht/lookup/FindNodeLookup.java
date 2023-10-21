package jtorrent.domain.manager.dht.lookup;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import jtorrent.domain.model.dht.message.response.FindNodeResponse;
import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.util.Bit160Value;

public class FindNodeLookup extends IterativeLookup<FindNodeResponse, Collection<Node>> {

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
                .map(Node::neverSeen)
                .collect(Collectors.toList());
    }

    @Override
    protected CompletableFuture<FindNodeResponse> doQuery(Node node, Bit160Value target) {
        return node.findNode(target);
    }
}
