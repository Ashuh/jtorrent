package jtorrent.domain.util;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public abstract class BackgroundTask implements Runnable {

    private static final Logger LOGGER = System.getLogger(BackgroundTask.class.getName());

    private final Thread thread = new Thread(this);
    private volatile boolean isRunning = false;

    @Override
    public final void run() {
        doOnStarted();
        while (isRunning) {
            try {
                execute();
            } catch (InterruptedException e) {
                LOGGER.log(Level.DEBUG, "Task interrupted: {0}", getClass().getName());
                Thread.currentThread().interrupt();
            }
        }
        doOnStopped();
        LOGGER.log(Level.DEBUG, "Task finished: {0}", getClass().getName());
    }

    protected abstract void execute() throws InterruptedException;

    public final void start() {
        LOGGER.log(Level.DEBUG, "Starting task: {0}", getClass().getName());
        isRunning = true;
        doOnStart();
        thread.start();
    }

    public final void stop() {
        LOGGER.log(Level.DEBUG, "Stopping task: {0}", getClass().getName());
        isRunning = false;
        doOnStop();
        thread.interrupt();
    }

    public boolean isRunning() {
        return isRunning;
    }

    protected void doOnStart() {
    }

    protected void doOnStarted() {
    }

    protected void doOnStop() {
    }

    protected void doOnStopped() {
    }
}
