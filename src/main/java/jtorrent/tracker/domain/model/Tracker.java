package jtorrent.tracker.domain.model;

import java.io.IOException;

import jtorrent.common.domain.util.Sha1Hash;

public interface Tracker {

    AnnounceResponse announce(Sha1Hash infoHash, long downloaded, long left, long uploaded, Event event)
            throws IOException;
}
