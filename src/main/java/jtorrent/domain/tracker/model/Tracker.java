package jtorrent.domain.tracker.model;

import java.io.IOException;
import java.net.URI;

import jtorrent.domain.common.util.Sha1Hash;

public interface Tracker {

    AnnounceResponse announce(Sha1Hash infoHash, long downloaded, long left, long uploaded, Event event)
            throws IOException;

    URI getUri();
}
