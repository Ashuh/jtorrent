package jtorrent.domain.dht.communication;

import jtorrent.domain.dht.model.message.error.Error;

public class DhtErrorException extends Exception {

    private static final String MESSAGE_FORMAT = "%s: %s";

    public DhtErrorException(Error error) {
        super(String.format(MESSAGE_FORMAT, error.getErrorCode(), error.getErrorMessage()));
    }
}
