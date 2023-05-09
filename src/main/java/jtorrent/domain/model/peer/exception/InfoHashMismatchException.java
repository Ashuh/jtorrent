package jtorrent.domain.model.peer.exception;

import jtorrent.domain.model.torrent.Sha1Hash;

public class InfoHashMismatchException extends RuntimeException {

    private static final String FORMAT = "Expected infohash %s but got %s";

    public InfoHashMismatchException(Sha1Hash expected, Sha1Hash actual) {
        super(String.format(FORMAT, expected, actual));
    }
}
