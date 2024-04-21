package jtorrent.domain.dht.handler.routingtable;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.dht.handler.DhtManager;
import jtorrent.domain.dht.handler.node.Node;
import jtorrent.domain.dht.model.node.NodeIdPrefix;

public class Bucket {

    public static final Logger LOGGER = LoggerFactory.getLogger(Bucket.class);

    private final NodeIdPrefix prefix;
    private final Set<Node> nodes;

    public Bucket(NodeIdPrefix prefix) {
        this.prefix = requireNonNull(prefix);
        this.nodes = new HashSet<>(DhtManager.K);
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public boolean addNode(Node node) {
        if (isFull()) {
            LOGGER.debug(Markers.DHT, "Bucket {} is full", getPrefixBitLength());
            Optional<Node> removedNode = evictBadOrQuestionableNode();
            if (removedNode.isPresent()) {
                LOGGER.debug(Markers.DHT, "Removed node {} from bucket {}", removedNode.get(), getPrefixBitLength());
                return nodes.add(node);
            }
            return false;
        }

        return nodes.add(node);
    }

    public boolean isFull() {
        return nodes.size() >= DhtManager.K;
    }

    public int getPrefixBitLength() {
        return prefix.getBitLength();
    }

    private Optional<Node> evictBadOrQuestionableNode() {
        Optional<Node> badNode = getBadNodes().stream().findAny();
        if (badNode.isPresent()) {
            nodes.remove(badNode.get());
            return badNode;
        }

        Optional<Node> questionableNode = getQuestionableNodes().stream()
                .filter(Predicate.not(Node::isContactable))
                .findFirst();

        if (questionableNode.isPresent()) {
            nodes.remove(questionableNode.get());
            return questionableNode;
        }

        return Optional.empty();
    }

    public List<Node> getBadNodes() {
        return nodes.stream()
                .filter(node -> node.getState() == Node.State.BAD)
                .collect(Collectors.toList());
    }

    public List<Node> getQuestionableNodes() {
        return nodes.stream()
                .filter(node -> node.getState() == Node.State.QUESTIONABLE)
                .collect(Collectors.toList());
    }

    public List<Node> getNodes() {
        return new ArrayList<>(nodes);
    }

    public NodeIdPrefix getPrefix() {
        return prefix;
    }

    public LocalDateTime getLastUpdated() {
        return nodes.stream()
                .map(Node::getLastSeen)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.MIN);
    }
}
