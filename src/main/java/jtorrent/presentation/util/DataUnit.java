package jtorrent.presentation.util;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNegative;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum DataUnit {

    BYTE(pow2(0), "B"),
    KILOBYTE(pow2(10), "KB"),
    MEGABYTE(pow2(20), "MB"),
    GIGABYTE(pow2(30), "GB"),
    TERABYTE(pow2(40), "TB"),
    PETABYTE(pow2(50), "PB"),
    EXABYTE(pow2(60), "EB");

    private static final Map<String, DataUnit> RATE_SYMBOL_TO_UNIT;

    static {
        RATE_SYMBOL_TO_UNIT = Arrays.stream(DataUnit.values())
                .collect(Collectors.toMap(DataUnit::getRateSymbol, Function.identity()));
    }

    /**
     * The number of bytes that this unit represents.
     */
    private final long numberOfBytes;
    /**
     * The symbol that represents this unit.
     */
    private final String symbol;

    DataUnit(long numberOfBytes, String symbol) {
        this.numberOfBytes = numberOfBytes;
        this.symbol = symbol;
    }

    private static long pow2(int exponent) {
        return (long) Math.pow(2, exponent);
    }

    /**
     * Gets the {@link DataUnit} that is the best fit for the given number of bytes.
     * <p>
     * The best fit is defined as the {@link DataUnit} that is the biggest unit that is smaller than the given
     * number of bytes. Example:
     * <ul>
     *     <li>For bytes in range [2^0, 2^10), the best fit is {@link DataUnit#BYTE}.</li>
     *     <li>For bytes in range [2^10, 2^20), the best fit is {@link DataUnit#KILOBYTE}.</li>
     *     <li>For bytes in range [2^20, 2^30), the best fit is {@link DataUnit#MEGABYTE}.</li>
     * </ul>
     * If the given number of bytes is 0, then {@link DataUnit#BYTE} is returned.
     *
     * @param bytes the number of bytes. Must be non-negative.
     * @return the {@link DataUnit} that is the best fit for the given number of bytes
     */
    public static DataUnit forBytes(long bytes) {
        requireNonNegative(bytes);
        if (bytes == 0) {
            return BYTE;
        }
        DataUnit[] units = DataUnit.values();
        double exponent = log2Floor(bytes);
        int index = (int) (Math.floor(exponent / 10));
        index = Math.min(index, units.length - 1);
        return units[index];
    }

    private static int log2Floor(long bytes) {
        int log = 0;
        if ((bytes & 0xFFFF_FFFF_0000_0000L) != 0) {
            bytes >>>= 32;
            log += 32;
        }
        if ((bytes & 0xFFFF_0000L) != 0) {
            bytes >>>= 16;
            log += 16;
        }
        if ((bytes & 0xFF00L) != 0) {
            bytes >>>= 8;
            log += 8;
        }
        if ((bytes & 0xF0L) != 0) {
            bytes >>>= 4;
            log += 4;
        }
        if ((bytes & 0b1100L) != 0) {
            bytes >>>= 2;
            log += 2;
        }
        if ((bytes & 0b10L) != 0) {
            log += 1;
        }
        return log;
    }

    public static DataUnit fromRateSymbol(String symbol) {
        DataUnit unit = RATE_SYMBOL_TO_UNIT.get(symbol);
        if (unit == null) {
            throw new IllegalArgumentException("No DataUnit for rate symbol: " + symbol);
        }
        return unit;
    }

    /**
     * Gets the symbol for this unit. Example: "B" for {@link DataUnit#BYTE}, "KB" for {@link DataUnit#KILOBYTE}, etc.
     *
     * @return the symbol for this unit
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Gets the symbol for this unit, with "/s" appended to it. Example: "B/s" for {@link DataUnit#BYTE}, "KB/s" for
     * {@link DataUnit#KILOBYTE}, etc.
     *
     * @return the symbol for this unit, with "/s" appended to it
     */
    public String getRateSymbol() {
        return symbol + "/s";
    }

    public long getNumberOfBytes() {
        return numberOfBytes;
    }

    /**
     * Converts the given size from the given unit to this unit.
     *
     * @param size the size to convert
     * @param from the unit of the given size
     * @return the size in this unit
     */
    public double convertFrom(double size, DataUnit from) {
        return size * from.numberOfBytes / numberOfBytes;
    }

    /**
     * Converts the given size from the given unit to this unit.
     *
     * @param size the size to convert
     * @param from the unit of the given size
     * @return the size in this unit
     */
    public long convertFrom(long size, DataUnit from) {
        return size * from.numberOfBytes / numberOfBytes;
    }

    /**
     * Converts the given size from this unit to the given unit.
     *
     * @param size the size to convert
     * @param to   the unit to convert to
     * @return the size in the given unit
     */
    public double convertTo(double size, DataUnit to) {
        return size * numberOfBytes / to.numberOfBytes;
    }

    /**
     * Converts the given size from this unit to the given unit.
     *
     * @param size the size to convert
     * @param to   the unit to convert to
     * @return the size in the given unit
     */
    public long convertTo(long size, DataUnit to) {
        return size * numberOfBytes / to.numberOfBytes;
    }
}
