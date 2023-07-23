package jtorrent.domain.util;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class DurationWindow {

    private final Duration windowDuration;
    private final Queue<Integer> window = new LinkedList<>();
    private long windowTotal = 0;
    private final Subject<Double> rate = BehaviorSubject.createDefault(0.0);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public DurationWindow(Duration windowDuration) {
        if (windowDuration.isNegative() || windowDuration.isZero()) {
            throw new IllegalArgumentException("Window duration must be positive");
        }
        this.windowDuration = windowDuration;
    }

    public synchronized void add(int value) {
        window.add(value);
        windowTotal += value;
        executor.schedule(this::remove, windowDuration.toMillis(), TimeUnit.MILLISECONDS);
        rate.onNext(getRate());
    }

    private synchronized void remove() {
        windowTotal -= window.remove();
        rate.onNext(getRate());
    }

    public double getRate() {
        return windowTotal / (windowDuration.toMillis() / 1000.0);
    }

    public Observable<Double> getRateObservable() {
        return rate;
    }
}
