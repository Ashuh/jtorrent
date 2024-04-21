package jtorrent.domain.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BackgroundTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundTask.class);

    private final Thread thread;
    private volatile State state = State.IDLE;

    protected BackgroundTask() {
        thread = new Thread(this);
        thread.setName(getThreadName());
    }

    protected String getThreadName() {
        return getClass().getName();
    }

    @Override
    public final void run() {
        synchronized (this) {
            if (state != State.STOPPING) {
                assert state == State.STARTING;
                state = State.STARTED;
                doOnStarted();
            }
        }

        while (state == State.STARTED) {
            try {
                execute();
            } catch (InterruptedException e) {
                LOGGER.debug("Task {} interrupted", getClass().getName(), e);
                Thread.currentThread().interrupt();
            }
        }

        state = State.STOPPED;
        doOnStopped();
    }

    protected abstract void execute() throws InterruptedException;

    protected void doOnStarted() {
    }

    protected void doOnStopped() {
    }

    public final synchronized void start() {
        if (state != State.IDLE) {
            boolean isStopRequested = state == State.STOPPING || state == State.STOPPED;
            if (isStopRequested) {
                LOGGER.error("Unable to start task {}, stop already requested", getClass().getName());
            } else {
                LOGGER.debug("Task {} is already starting or started", getClass().getName());
            }
            return;
        }

        LOGGER.debug("Starting task: {}", getClass().getName());
        state = State.STARTING;
        doOnStart();
        thread.start();
    }

    protected void doOnStart() {
    }

    public final synchronized void stop() {
        if (state != State.STARTING && state != State.STARTED) {
            LOGGER.debug("Task {} is not running or is already stopping", getClass().getName());
            return;
        }

        LOGGER.debug("Stopping task {}", getClass().getName());
        state = State.STOPPING;
        doOnStop();
        thread.interrupt();
    }

    protected void doOnStop() {
    }

    public boolean isStopping() {
        return state == State.STOPPING;
    }

    private enum State {
        IDLE,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED
    }
}
