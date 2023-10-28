package jtorrent.domain.model.dht.message;

public class DhtDecodingException extends Exception {

    public DhtDecodingException() {
    }

    public DhtDecodingException(String message) {
        super(message);
    }

    public DhtDecodingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DhtDecodingException(Throwable cause) {
        super(cause);
    }

    public DhtDecodingException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
