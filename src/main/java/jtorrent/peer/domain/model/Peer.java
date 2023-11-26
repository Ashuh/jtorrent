package jtorrent.peer.domain.model;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import jtorrent.common.domain.util.DurationWindow;

public class Peer {

    private static final Logger LOGGER = System.getLogger(Peer.class.getName());

    private final PeerContactInfo peerContactInfo;
    private final DurationWindow durationWindow = new DurationWindow(Duration.ofSeconds(20));
    private boolean isLocalChoked = true;
    private boolean isRemoteChoked = true;
    private boolean isLocalInterested = false;
    private boolean isRemoteInterested = false;
    private LocalDateTime lastSeen = LocalDateTime.MIN;

    public Peer(PeerContactInfo peerContactInfo) {
        this.peerContactInfo = requireNonNull(peerContactInfo);
    }

    public void disconnect() {
        durationWindow.close();
    }

    public PeerContactInfo getPeerContactInfo() {
        return peerContactInfo;
    }

    public InetSocketAddress getSocketAddress() {
        return peerContactInfo.toInetSocketAddress();
    }

    public InetAddress getAddress() {
        return peerContactInfo.getAddress();
    }

    public int getPort() {
        return peerContactInfo.getPort();
    }

    public boolean isLocalChoked() {
        return isLocalChoked;
    }

    public void setLocalChoked(boolean localChoked) {
        isLocalChoked = localChoked;
    }

    public boolean isRemoteChoked() {
        return isRemoteChoked;
    }

    public void setRemoteChoked(boolean remoteChoked) {
        isRemoteChoked = remoteChoked;
    }

    public boolean isLocalInterested() {
        return isLocalInterested;
    }

    public void setLocalInterested(boolean isLocalInterested) {
        this.isLocalInterested = isLocalInterested;
    }

    public boolean isRemoteInterested() {
        return isRemoteInterested;
    }

    public void setRemoteInterested(boolean isRemoteInterested) {
        this.isRemoteInterested = isRemoteInterested;
    }

    public double getDownloadRate() {
        return durationWindow.getRate();
    }

    public void addDownloadedBytes(int bytes) {
        durationWindow.add(bytes);
    }

    public Observable<Double> getDownloadRateObservable() {
        return durationWindow.getRateObservable();
    }

    public double getUploadRate() {
        return 0; // TODO: implement
    }

    public Observable<Double> getUploadRateObservable() {
        return Observable.never(); // TODO: implement
    }

    public boolean isLastSeenWithin(Duration duration) {
        return lastSeen.isAfter(LocalDateTime.now().minus(duration));
    }

    public void setLastSeenNow() {
        lastSeen = LocalDateTime.now();
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerContactInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Peer peer = (Peer) o;
        return peerContactInfo.equals(peer.peerContactInfo);
    }

    @Override
    public String toString() {
        return peerContactInfo.toString();
    }
}
