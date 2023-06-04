package jtorrent.domain.model.tracker;

import java.io.IOException;

import jtorrent.domain.model.torrent.Torrent;

public interface Tracker {

    AnnounceResponse announce(Torrent torrent, Event event) throws IOException;
}
