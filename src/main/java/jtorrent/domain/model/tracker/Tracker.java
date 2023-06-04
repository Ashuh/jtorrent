package jtorrent.domain.model.tracker;

import java.io.IOException;

import jtorrent.domain.model.torrent.Sha1Hash;

public interface Tracker {

    AnnounceResponse announce(Sha1Hash infoHash, long downloaded, long left, long uploaded, Event event)
            throws IOException;
}
