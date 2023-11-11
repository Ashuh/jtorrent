package jtorrent.peer.domain.model.exception;

import java.io.IOException;

public class UnexpectedEndOfStreamException extends IOException {

    public UnexpectedEndOfStreamException() {
    }

    public UnexpectedEndOfStreamException(String message) {
        super(message);
    }
}
