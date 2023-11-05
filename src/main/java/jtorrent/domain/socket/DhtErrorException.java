package jtorrent.domain.socket;

import jtorrent.domain.model.dht.message.error.Error;

public class DhtErrorException extends Exception {

    private static final String MESSAGE_FORMAT = "%s: %s";

    public DhtErrorException(Error error) {
        super(String.format(MESSAGE_FORMAT, error.getErrorCode(), error.getErrorMessage()));
    }
}
