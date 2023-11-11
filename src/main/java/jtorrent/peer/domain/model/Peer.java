package jtorrent.peer.domain.model;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import jtorrent.common.domain.util.DurationWindow;

public class Peer {

    private static final Logger LOGGER = System.getLogger(Peer.class.getName());

    protected final PeerContactInfo peerContactInfo;
    protected final DurationWindow durationWindow = new DurationWindow(Duration.ofSeconds(20));
    protected boolean isLocalChoked = true;
    protected boolean isRemoteChoked = true;

    public Peer(PeerContactInfo peerContactInfo) {
        this.peerContactInfo = requireNonNull(peerContactInfo);
    }

    public void disconnect() {
        durationWindow.close();
    }

    public InetAddress getAddress() {
        return getPeerContactInfo().getAddress();
    }

    public PeerContactInfo getPeerContactInfo() {
        return peerContactInfo;
    }

    public int getPort() {
        return getPeerContactInfo().getPort();
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
