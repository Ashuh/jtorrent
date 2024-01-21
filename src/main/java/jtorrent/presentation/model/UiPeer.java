package jtorrent.presentation.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.domain.peer.model.Peer;
import jtorrent.presentation.util.UpdatePropertyConsumer;

public class UiPeer {

    private final StringProperty ip;
    private final StringProperty port;
    private final StringProperty client;
    private final DoubleProperty downSpeed;
    private final DoubleProperty upSpeed;

    public UiPeer(StringProperty ip, StringProperty port, StringProperty client, DoubleProperty downSpeed,
            DoubleProperty upSpeed) {
        this.ip = requireNonNull(ip);
        this.port = requireNonNull(port);
        this.client = requireNonNull(client);
        this.downSpeed = requireNonNull(downSpeed);
        this.upSpeed = requireNonNull(upSpeed);
    }

    public static UiPeer fromDomain(Peer peer) {
        SimpleStringProperty ip = new SimpleStringProperty(peer.getAddress().getHostAddress());
        SimpleStringProperty port = new SimpleStringProperty(String.valueOf(peer.getPort()));
        SimpleStringProperty client = new SimpleStringProperty("Placeholder");
        SimpleDoubleProperty downSpeed = new SimpleDoubleProperty(0.0);
        SimpleDoubleProperty upSpeed = new SimpleDoubleProperty(0.0);

        peer.getDownloadRateObservable().subscribe(new UpdatePropertyConsumer<>(downSpeed));
        peer.getUploadRateObservable().subscribe(new UpdatePropertyConsumer<>(upSpeed));

        return new UiPeer(ip, port, client, downSpeed, upSpeed);
    }

    public String getIp() {
        return ip.get();
    }

    public StringProperty ipProperty() {
        return ip;
    }

    public String getPort() {
        return port.get();
    }

    public StringProperty portProperty() {
        return port;
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
