package jtorrent.common.domain.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class RangeListTest {

    private static final long RANGE_START = -10;
    private static final long RANGE_SIZE_0 = 10;
    private static final long RANGE_SIZE_1 = 20;
    private static final long RANGE_SIZE_2 = 30;

    public static final long RANGE_END_0 = RANGE_START + RANGE_SIZE_0;
    public static final long RANGE_END_1 = RANGE_END_0 + RANGE_SIZE_1;
    public static final long RANGE_END_2 = RANGE_END_1 + RANGE_SIZE_2;

    private static final long RANGE_START_1 = RANGE_END_0;
    private static final long RANGE_START_2 = RANGE_END_1;

    private static final RangeList RANGE_LIST = new RangeList(
            List.of(RANGE_START, RANGE_START_1, RANGE_START_2, RANGE_END_2));

    @Test
    void fromRangeSizes() {
        RangeList actual = RangeList.fromRangeSizes(RANGE_START, List.of(RANGE_SIZE_0, RANGE_SIZE_1, RANGE_SIZE_2));
        assertEquals(RANGE_LIST, actual);
    }

    @Test
    void getRangeStart_indexWithinBounds_success() {
        assertEquals(RANGE_START, RANGE_LIST.getRangeStart(0));
        assertEquals(RANGE_START_1, RANGE_LIST.getRangeStart(1));
        assertEquals(RANGE_START_2, RANGE_LIST.getRangeStart(2));
    }

    @Test
    void getRangeStart_indexOutOfBounds_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> RANGE_LIST.getRangeStart(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> RANGE_LIST.getRangeStart(3));
    }

    @Test
    void getRangeEnd_indexWithinBounds_success() {
        assertEquals(RANGE_END_0, RANGE_LIST.getRangeEnd(0));
        assertEquals(RANGE_END_1, RANGE_LIST.getRangeEnd(1));
        assertEquals(RANGE_END_2, RANGE_LIST.getRangeEnd(2));
    }

    @Test
    void getRangeEnd_indexOutOfBounds_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> RANGE_LIST.getRangeEnd(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> RANGE_LIST.getRangeEnd(3));
    }

    @Test
    void getRangeIndex_valueWithinRange_success() {
        assertEquals(0, RANGE_LIST.getRangeIndex(RANGE_START));
        assertEquals(0, RANGE_LIST.getRangeIndex(RANGE_END_0 - 1));
        assertEquals(1, RANGE_LIST.getRangeIndex(RANGE_START_1));
        assertEquals(1, RANGE_LIST.getRangeIndex(RANGE_END_1 - 1));
        assertEquals(2, RANGE_LIST.getRangeIndex(RANGE_START_2));
        assertEquals(2, RANGE_LIST.getRangeIndex(RANGE_END_2 - 1));
    }

    @Test
    void getRangeIndex_valueOutOfBounds_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> RANGE_LIST.getRangeIndex(RANGE_START - 1));
        assertThrows(IllegalArgumentException.class, () -> RANGE_LIST.getRangeIndex(RANGE_END_2));
    }
}
