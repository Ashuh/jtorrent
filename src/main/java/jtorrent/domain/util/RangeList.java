package jtorrent.domain.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RangeList {

    private final List<Integer> boundaries;

    public RangeList(List<Integer> boundaries) {
        this.boundaries = Collections.unmodifiableList(boundaries);
    }

    public static RangeList fromRangeSizes(int start, List<Integer> rangeSizes) {
        Integer[] boundaries = new Integer[rangeSizes.size() + 1];
        boundaries[0] = start;
        for (int i = 0; i < rangeSizes.size(); i++) {
            boundaries[i + 1] = boundaries[i] + rangeSizes.get(i);
        }

        return new RangeList(Arrays.asList(boundaries));
    }

    public int getRangeStart(int index) {
        if (index < 0 || index >= boundaries.size() - 1) {
            throw new IndexOutOfBoundsException(index);
        }
        return boundaries.get(index);
    }

    public int getRangeEnd(int index) {
        if (index < 0 || index >= boundaries.size() - 1) {
            throw new IndexOutOfBoundsException(index);
        }
        return boundaries.get(index + 1);
    }

    public int getRangeIndex(int value) {
        if (value < boundaries.get(0)) {
            throw new IllegalArgumentException("Value is smaller than the first boundary");
        }

        if (value >= boundaries.get(boundaries.size() - 1)) {
            throw new IllegalArgumentException("Value is larger than the last boundary");
        }

        int low = 0;
        int high = boundaries.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int boundary = boundaries.get(mid);

            if (value == boundary) {
                return mid;
            } else if (value < boundary) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return high;
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundaries);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RangeList rangeList = (RangeList) o;
        return Objects.equals(boundaries, rangeList.boundaries);
    }

    @Override
    public String toString() {
        return "RangeList{"
                + "boundaries="
                + boundaries
                + '}';
    }
}
