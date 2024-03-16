package jtorrent.presentation.common.util;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNegative;
import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.text.DecimalFormat;

public record DataSize(double size, DataUnit unit) {

    private static final DecimalFormat FORMAT_SIZE = new DecimalFormat("#.##");

    public DataSize(double size, DataUnit unit) {
        this.size = requireNonNegative(size);
        this.unit = requireNonNull(unit);
    }

    public static DataSize fromRateString(String string) {
        String[] parts = string.split(" ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid rate string: " + string);
        }
        return new DataSize(Double.parseDouble(parts[0]), DataUnit.fromRateSymbol(parts[1]));
    }

    /**
     * Creates a new {@link DataSize} from the given size specified in bytes, converting the size to the best fit unit.
     * This method is equivalent to calling {@link #bestFit(double, DataUnit)} with {@link DataUnit#BYTE}.
     *
     * @param size the size of the data in bytes
     * @return the best fit {@link DataSize}
     */
    public static DataSize bestFitBytes(double size) {
        return bestFit(size, DataUnit.BYTE);
    }

    /**
     * Creates a new {@link DataSize} from the given size and unit, converting the size to the best fit unit.
     * Refer to {@link DataUnit#bestFitBytes} for details on how the best fit unit is determined.
     *
     * @param size the size of the data
     * @param unit the unit of the data
     * @return the best fit {@link DataSize}
     */
    public static DataSize bestFit(double size, DataUnit unit) {
        double sizeInBytes = size * unit.getNumberOfBytes();
        DataUnit bestFit = DataUnit.bestFitBytes((long) sizeInBytes);
        double convertedSize = sizeInBytes / bestFit.getNumberOfBytes();
        return new DataSize(convertedSize, bestFit);
    }

    public double getSizeInBytes() {
        return size * unit.getNumberOfBytes();
    }

    /**
     * Converts this data size to a string that represents the rate of data transfer. Example: "1.23 MB/s"
     *
     * @return the rate string
     */
    public String toRateString() {
        return this + "/s";
    }

    /**
     * Converts this data size to a string that represents the size. Example: "1.23 MB"
     *
     * @return the size string
     */
    @Override
    public String toString() {
        return FORMAT_SIZE.format(size) + " " + unit.getSymbol();
    }
}
