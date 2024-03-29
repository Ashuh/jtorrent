package jtorrent.common.domain.util;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public abstract class BackgroundTask implements Runnable {

    private static final Logger LOGGER = System.getLogger(BackgroundTask.class.getName());

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
                LOGGER.log(Level.DEBUG, "Task interrupted: {0}", getClass().getName());
                Thread.currentThread().interrupt();
            }
        }

        state = State.STOPPED;
        doOnStopped();
        LOGGER.log(Level.DEBUG, "Task finished: {0}", getClass().getName());
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
                LOGGER.log(Level.ERROR, "Unable to start task: {0}, stop already requested", getClass().getName());
            } else {
                LOGGER.log(Level.DEBUG, "Task: {0} is already starting or started", getClass().getName());
            }
            return;
        }

        LOGGER.log(Level.DEBUG, "Starting task: {0}", getClass().getName());
        state = State.STARTING;
        doOnStart();
        thread.start();
    }

    protected void doOnStart() {
    }

    public final synchronized void stop() {
        if (state != State.STARTING && state != State.STARTED) {
            LOGGER.log(Level.DEBUG, "Task: {0} is not running or is already stopping", getClass().getName());
            return;
        }

        LOGGER.log(Level.DEBUG, "Stopping task: {0}", getClass().getName());
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
