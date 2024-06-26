package jtorrent.domain.tracker.handler.exception;

public class ExceededMaxTriesException extends RuntimeException {

    private static final String FORMAT = "%s failed after %d tries";

    public ExceededMaxTriesException(String task, int maxTries) {
        super(String.format(FORMAT, task, maxTries));
    }
}
