package jtorrent.presentation.util;

import java.text.DecimalFormat;

public class DataUnitFormatter {

    private static final DecimalFormat FORMAT_SIZE = new DecimalFormat("#.##");
    private static final DecimalFormat FORMAT_RATE = new DecimalFormat("#.##");

    private DataUnitFormatter() {
    }

    public static String formatSize(long bytes) {
        DataUnit unit = DataUnit.forBytes(bytes);
        double value = unit.convertFrom(bytes, DataUnit.BYTE);
        return FORMAT_SIZE.format(value) + " " + unit.getSymbol();
    }

    public static String formatRate(double bytes) {
        DataUnit unit = DataUnit.forBytes((long) bytes);
        double value = unit.convertFrom(bytes, DataUnit.BYTE);
        return FORMAT_RATE.format(value) + " " + unit.getSymbol() + "/s";
    }
}
