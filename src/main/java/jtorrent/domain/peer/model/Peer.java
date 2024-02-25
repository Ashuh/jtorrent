package jtorrent.domain.peer.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import jtorrent.domain.common.util.RateTracker;

public class Peer {

    private static final Logger LOGGER = System.getLogger(Peer.class.getName());
    private static final Duration WINDOW_DURATION = Duration.ofSeconds(20);

    private final PeerContactInfo peerContactInfo;
    private final RateTracker downloadRateTracker = new RateTracker(WINDOW_DURATION);
    private final RateTracker uploadRateTracker = new RateTracker(WINDOW_DURATION);
    private final Observable<Double> downloadRateObservable =
            downloadRateTracker.getRateObservable(1, TimeUnit.SECONDS); // TODO: fixed rate?
    private final Observable<Double> uploadRateObservable =
            uploadRateTracker.getRateObservable(1, TimeUnit.SECONDS); // TODO: fixed rate?
    private boolean isLocalChoked = true;
    private boolean isRemoteChoked = true;
    private boolean isLocalInterested = false;
    private boolean isRemoteInterested = false;
    private LocalDateTime lastSeen = LocalDateTime.MIN;

    public Peer(PeerContactInfo peerContactInfo) {
        this.peerContactInfo = requireNonNull(peerContactInfo);
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

    public void addDownloadedBytes(int bytes) {
        downloadRateTracker.addBytes(bytes);
    }

    public double getDownloadRate() {
        return downloadRateTracker.getRate();
    }

    public Observable<Double> getDownloadRateObservable() {
        return downloadRateObservable;
    }

    public void addUploadedBytes(int bytes) {
        uploadRateTracker.addBytes(bytes);
        System.out.println("Added " + bytes + " bytes to upload rate tracker");
    }

    public double getUploadRate() {
        return uploadRateTracker.getRate();
    }

    public Observable<Double> getUploadRateObservable() {
        return uploadRateObservable;
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
