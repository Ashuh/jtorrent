package jtorrent.presentation.util;

import java.time.Duration;

import io.reactivex.rxjava3.functions.BiFunction;

public class CalculateEtaCombiner implements BiFunction<Long, Double, String> {

    private static final String INFINITY = "\u221E";

    private final long size;

    public CalculateEtaCombiner(long size) {
        this.size = size;
    }

    @Override
    public String apply(Long downloaded, Double rate) {
        if (downloaded == size) {
            return "";
        }

        if (rate == 0) {
            return INFINITY;
        } else {
            long etaSeconds = (long) ((size - downloaded) / rate);
            return formatTime(etaSeconds);
        }
    }

    private static String formatTime(long seconds) {
        Duration duration = Duration.ofSeconds(seconds);
        long days = duration.toDaysPart();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long secs = duration.toSecondsPart();
        return String.format("%sd %sh %sm %ss", days, hours, minutes, secs);
    }
}
