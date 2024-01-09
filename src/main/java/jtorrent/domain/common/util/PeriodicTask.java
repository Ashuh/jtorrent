package jtorrent.domain.common.util;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class PeriodicTask implements Runnable {

    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> scheduledFuture;

    protected PeriodicTask(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = requireNonNull(scheduledExecutorService);
    }

    public void scheduleWithFixedDelay(long delay, TimeUnit timeUnit) {
        scheduleWithFixedDelay(0, delay, timeUnit);
    }

    public void scheduleWithFixedDelay(long initialDelay, long delay, TimeUnit timeUnit) {
        checkNotRunning();
        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this, initialDelay, delay, timeUnit);
    }

    public void scheduleAtFixedRate(long period, TimeUnit timeUnit) {
        scheduleAtFixedRate(0, period, timeUnit);
    }

    public void scheduleAtFixedRate(long initialDelay, long period, TimeUnit timeUnit) {
        checkNotRunning();
        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this, initialDelay, period, timeUnit);
    }

    private void checkNotRunning() {
        if (isRunning()) {
            throw new IllegalStateException("Task already running");
        }
    }

    public void stop() {
        if (!isRunning()) {
            return;
        }

        scheduledFuture.cancel(true);
    }

    private boolean isRunning() {
        return scheduledFuture != null && !scheduledFuture.isCancelled();
    }
}
