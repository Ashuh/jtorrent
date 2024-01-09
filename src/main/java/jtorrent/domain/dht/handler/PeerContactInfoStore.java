package jtorrent.domain.dht.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.peer.model.PeerContactInfo;

public class PeerContactInfoStore {

    private final Map<Sha1Hash, Set<PeerContactInfo>> infoHashToPeerContactInfo = new HashMap<>();

    public void addPeerContactInfo(Sha1Hash infoHash, PeerContactInfo peerContactInfo) {
        infoHashToPeerContactInfo
                .computeIfAbsent(infoHash, k -> new HashSet<>())
                .add(peerContactInfo);
    }

    public Collection<PeerContactInfo> getPeerContactInfos(Sha1Hash infoHash) {
        return infoHashToPeerContactInfo.getOrDefault(infoHash, Collections.emptySet());
    }
}
