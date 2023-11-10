package jtorrent.peer.presentation;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.common.presentation.util.UpdatePropertyConsumer;
import jtorrent.peer.domain.model.Peer;

public class UiPeer {

    private final StringProperty ip;
    private final StringProperty client;
    private final DoubleProperty downSpeed;
    private final DoubleProperty upSpeed;

    public UiPeer(StringProperty ip, StringProperty client, DoubleProperty downSpeed, DoubleProperty upSpeed) {
        this.ip = requireNonNull(ip);
        this.client = requireNonNull(client);
        this.downSpeed = requireNonNull(downSpeed);
        this.upSpeed = requireNonNull(upSpeed);
    }

    public static UiPeer fromDomain(Peer peer) {
        SimpleStringProperty ip = new SimpleStringProperty(peer.getAddress().getHostAddress());
        SimpleStringProperty client = new SimpleStringProperty("Placeholder");
        SimpleDoubleProperty downSpeed = new SimpleDoubleProperty(0.0);
        SimpleDoubleProperty upSpeed = new SimpleDoubleProperty(0.0);

        peer.getDownloadRateObservable().subscribe(new UpdatePropertyConsumer<>(downSpeed));
        peer.getDownloadRateObservable().subscribe(new UpdatePropertyConsumer<>(upSpeed));

        return new UiPeer(ip, client, downSpeed, upSpeed);
    }

    public String getIp() {
        return ip.get();
    }

    public StringProperty ipProperty() {
        return ip;
    }

    public String getClient() {
        return client.get();
    }

    public StringProperty clientProperty() {
        return client;
    }

    public double getDownSpeed() {
        return downSpeed.get();
    }

    public DoubleProperty downSpeedProperty() {
        return downSpeed;
    }

    public double getUpSpeed() {
        return upSpeed.get();
    }

    public DoubleProperty upSpeedProperty() {
        return upSpeed;
    }
}
