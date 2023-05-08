package jtorrent.domain.model.peer.exception;

public class IncompleteReadException extends RuntimeException {

    private static final String FORMAT = "Expected %d bytes but got %d";

    public IncompleteReadException(int expected, int actual) {
        super(String.format(FORMAT, expected, actual));
    }
}
