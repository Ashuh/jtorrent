package jtorrent.common.domain.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class PeriodicTask implements Runnable {

    private final ScheduledExecutorService scheduledExecutorService;
    private final long interval;
    private final TimeUnit timeUnit;
    private ScheduledFuture<?> scheduledFuture;

    protected PeriodicTask(ScheduledExecutorService scheduledExecutorService, long interval,
            TimeUnit timeUnit) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.interval = interval;
        this.timeUnit = timeUnit;
    }

    public void start() {
        if (isRunning()) {
            throw new IllegalStateException("Task already running");
        }

        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this, 0, interval, timeUnit);
    }

    public void stop() {
        if (!isRunning()) {
            throw new IllegalStateException("Task not running");
        }

        scheduledFuture.cancel(true);
    }

    private boolean isRunning() {
        return scheduledFuture != null && !scheduledFuture.isCancelled();
    }
}
