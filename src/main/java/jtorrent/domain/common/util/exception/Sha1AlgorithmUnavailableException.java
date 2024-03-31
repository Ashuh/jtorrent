package jtorrent.domain.common.util.exception;

public class Sha1AlgorithmUnavailableException extends RuntimeException {

    public Sha1AlgorithmUnavailableException(Throwable cause) {
        super("SHA-1 algorithm is not available", cause);
    }
}
