package jtorrent.domain.util;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

public class DurationWindow {

    private final Queue<Integer> valueWindow = new LinkedList<>();

    private final Queue<Instant> timestampWindow = new LinkedList<>();

    private final Duration windowDuration;

    private long total = 0;

    public DurationWindow(Duration windowDuration) {
        if (windowDuration.isNegative() || windowDuration.isZero()) {
            throw new IllegalArgumentException("Window duration must be positive");
        }
        this.windowDuration = windowDuration;
    }

    public synchronized void add(int value) {
        timestampWindow.add(Instant.now());
        valueWindow.add(value);
        total += value;
        clearOldValues();
    }

    public synchronized long getWindowTotal() {
        clearOldValues();
        return total;
    }

    public synchronized double getWindowAverageRate() {
        clearOldValues();

        if (timestampWindow.isEmpty()) {
            return 0;
        }

        Instant windowStart = timestampWindow.peek();
        Instant windowEnd = Instant.now();

        long windowDurationMillis = Duration.between(windowStart, windowEnd).toMillis();
        double windowDurationSeconds = windowDurationMillis / 1000.0;
        return total / windowDurationSeconds;
    }

    private void clearOldValues() {
        Instant now = Instant.now();
        Instant threshold = now.minus(windowDuration);

        while (!timestampWindow.isEmpty() && timestampWindow.peek().isBefore(threshold)) {
            timestampWindow.remove();
            total -= valueWindow.remove();
        }
    }
}
