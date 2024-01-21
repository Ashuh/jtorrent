package jtorrent.presentation.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.domain.peer.model.Peer;
import jtorrent.presentation.util.BindingUtils;
import jtorrent.presentation.util.DataUnitFormatter;

public class UiPeer {

    private final StringProperty ip;
    private final StringProperty port;
    private final StringProperty client;
    private final StringProperty downSpeed;
    private final StringProperty upSpeed;
    private final CompositeDisposable disposables;

    public UiPeer(StringProperty ip, StringProperty port, StringProperty client, StringProperty downSpeed,
            StringProperty upSpeed, CompositeDisposable disposables) {
        this.ip = requireNonNull(ip);
        this.port = requireNonNull(port);
        this.client = requireNonNull(client);
        this.downSpeed = requireNonNull(downSpeed);
        this.upSpeed = requireNonNull(upSpeed);
        this.disposables = requireNonNull(disposables);
    }

    public static UiPeer fromDomain(Peer peer) {
        SimpleStringProperty ip = new SimpleStringProperty(peer.getAddress().getHostAddress());
        SimpleStringProperty port = new SimpleStringProperty(String.valueOf(peer.getPort()));
        SimpleStringProperty client = new SimpleStringProperty("Placeholder");
        SimpleStringProperty downSpeed = new SimpleStringProperty("");
        SimpleStringProperty upSpeed = new SimpleStringProperty("");
        CompositeDisposable disposables = new CompositeDisposable();

        Observable<Double> downloadRateObservable = peer.getDownloadRateObservable();
        BindingUtils.subscribe(downloadRateObservable.map(DataUnitFormatter::formatRate), downSpeed, disposables);

        Observable<Double> uploadRateObservable = peer.getUploadRateObservable();
        BindingUtils.subscribe(uploadRateObservable.map(DataUnitFormatter::formatRate), upSpeed, disposables);

        return new UiPeer(ip, port, client, downSpeed, upSpeed, disposables);
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

    public String getDownSpeed() {
        return downSpeed.get();
    }

    public StringProperty downSpeedProperty() {
        return downSpeed;
    }

    public String getUpSpeed() {
        return upSpeed.get();
    }

    public StringProperty upSpeedProperty() {
        return upSpeed;
    }

    public void dispose() {
        disposables.dispose();
    }
}
