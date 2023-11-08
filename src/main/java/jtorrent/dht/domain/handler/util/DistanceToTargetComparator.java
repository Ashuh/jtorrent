package jtorrent.dht.domain.handler.util;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.math.BigInteger;
import java.util.Comparator;

import jtorrent.dht.domain.handler.node.Node;
import jtorrent.dht.domain.model.node.NodeId;
import jtorrent.common.domain.util.Bit160Value;

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
