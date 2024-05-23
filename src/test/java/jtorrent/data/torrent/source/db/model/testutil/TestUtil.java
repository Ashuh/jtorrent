package jtorrent.data.torrent.source.db.model.testutil;

import java.util.BitSet;

public class TestUtil {

    private TestUtil() {
    }

    public static BitSet createBitSet(int... bits) {
        BitSet bitSet = new BitSet();
        for (int bit : bits) {
            bitSet.set(bit);
        }
        return bitSet;
    }

    public static BitSet createBitSetWithRange(int from, int to) {
        BitSet bitSet = new BitSet();
        bitSet.set(from, to);
        return bitSet;
    }
}
