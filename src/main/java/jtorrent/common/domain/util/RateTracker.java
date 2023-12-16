package jtorrent.common.domain.util;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * The RateTracker class is used to track the rate of data transfer in a sliding time window.
 * It keeps track of the total number of bytes transferred within the window duration, and provides
 * methods to add bytes to the tracker and get the current rate of data transfer. The rate is calculated
 * by dividing the total number of bytes within the window by the time elapsed since the oldest data point timestamp.
 * The RateTracker class also provides an Observable that emits the current rate at regular intervals.
 * <p>
 * Example usage:
 * <pre>{@code
 * RateTracker rateTracker = new RateTracker(Duration.ofSeconds(10));
 * rateTracker.addBytes(1000);
 * rateTracker.addBytes(2000);
 * double currentRate = rateTracker.getRate();
 * Observable<Double> rateObservable = rateTracker.getRateObservable(1, TimeUnit.SECONDS);
 * }</pre>
 */
public class RateTracker {

    private final Duration windowDuration;
    private final Queue<DataPoint> queue = new LinkedList<>();
    private long totalBytesInWindow = 0;

    public RateTracker(Duration windowDuration) {
        if (windowDuration.isNegative() || windowDuration.isZero()) {
            throw new IllegalArgumentException("Window duration must be positive");
        }
        this.windowDuration = windowDuration;
    }

    public synchronized void addBytes(long bytes) {
        DataPoint dataPoint = new DataPoint(bytes, System.currentTimeMillis());
        queue.add(dataPoint);
        totalBytesInWindow += bytes;
        cleanOldEntries();
    }

    private void cleanOldEntries() {
        long currentTimeMillis = System.currentTimeMillis();
        while (!queue.isEmpty() && currentTimeMillis - queue.peek().timestamp > windowDuration.toMillis()) {
            DataPoint oldest = queue.poll();
            totalBytesInWindow -= oldest.bytes;
        }
    }

    public Observable<Double> getRateObservable(long period, TimeUnit timeUnit) {
        return Observable.interval(period, timeUnit, Schedulers.computation())
                .map(tick -> getRate())
                .share();
    }

    public synchronized double getRate() {
        cleanOldEntries();
        if (queue.isEmpty()) {
            return 0;
        }
        long timeWindowMillis = System.currentTimeMillis() - queue.peek().timestamp;
        double timeWindowSeconds = timeWindowMillis / 1000.0;
        return totalBytesInWindow / timeWindowSeconds;
    }

    private static class DataPoint {

        private final long bytes;
        private final long timestamp;

        public DataPoint(long bytes, long timestamp) {
            this.bytes = bytes;
            this.timestamp = timestamp;
        }
    }
}
