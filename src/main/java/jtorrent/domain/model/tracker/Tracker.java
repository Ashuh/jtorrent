package jtorrent.domain.model.tracker;

import java.io.IOException;
import java.util.List;

import jtorrent.domain.model.torrent.Sha1Hash;
import jtorrent.domain.model.tracker.udp.message.PeerResponse;

public abstract class Tracker {

    public abstract List<PeerResponse> getPeers(Sha1Hash infoHash) throws IOException;
}
