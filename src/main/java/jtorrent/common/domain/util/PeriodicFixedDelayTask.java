package jtorrent.common.domain.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class PeriodicFixedDelayTask implements Runnable {

    private final ScheduledExecutorService scheduledExecutorService;
    private final long interval;
    private final TimeUnit timeUnit;
    private ScheduledFuture<?> scheduledFuture;

    protected PeriodicFixedDelayTask(ScheduledExecutorService scheduledExecutorService, long interval,
            TimeUnit timeUnit) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.interval = interval;
        this.timeUnit = timeUnit;
    }

    public void start() {
        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this, 0, interval, timeUnit);
    }

    public void stop() {
        scheduledFuture.cancel(true);
    }
}