package jtorrent.domain.util;

import static jtorrent.domain.util.ValidationUtil.requireNonNull;

import java.math.BigInteger;
import java.util.Comparator;

import jtorrent.domain.model.dht.node.Node;
import jtorrent.domain.model.dht.node.NodeId;

/**
 * Compares {@link Node}s by the distance of their {@link NodeId} to a {@link Bit160Value} target.
 */
public class DistanceToTargetComparator implements Comparator<Node> {

    private final Bit160Value target;

    public DistanceToTargetComparator(Bit160Value target) {
        this.target = requireNonNull(target);
    }

    @Override
    public int compare(Node o1, Node o2) {
        BigInteger dist1 = o1.getId().distanceTo(target);
        BigInteger dist2 = o2.getId().distanceTo(target);
        return dist1.compareTo(dist2);
    }
}
