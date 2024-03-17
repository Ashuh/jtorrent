package jtorrent.presentation.main.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import jtorrent.domain.peer.model.Peer;
import jtorrent.presentation.common.util.BindingUtils;
import jtorrent.presentation.common.util.DataSize;

public class UiPeer {

    private final ReadOnlyStringWrapper ip;
    private final ReadOnlyStringWrapper port;
    private final ReadOnlyStringWrapper client;
    private final ReadOnlyStringWrapper downSpeed;
    private final ReadOnlyStringWrapper upSpeed;
    private final CompositeDisposable disposables;

    public UiPeer(ReadOnlyStringWrapper ip, ReadOnlyStringWrapper port, ReadOnlyStringWrapper client,
            ReadOnlyStringWrapper downSpeed, ReadOnlyStringWrapper upSpeed, CompositeDisposable disposables) {
        this.ip = requireNonNull(ip);
        this.port = requireNonNull(port);
        this.client = requireNonNull(client);
        this.downSpeed = requireNonNull(downSpeed);
        this.upSpeed = requireNonNull(upSpeed);
        this.disposables = requireNonNull(disposables);
    }

    public static UiPeer fromDomain(Peer peer) {
        ReadOnlyStringWrapper ip = new ReadOnlyStringWrapper(peer.getAddress().getHostAddress());
        ReadOnlyStringWrapper port = new ReadOnlyStringWrapper(String.valueOf(peer.getPort()));
        ReadOnlyStringWrapper client = new ReadOnlyStringWrapper("Placeholder");
        ReadOnlyStringWrapper downSpeed = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper upSpeed = new ReadOnlyStringWrapper("");
        CompositeDisposable disposables = new CompositeDisposable();

        Observable<Double> downloadRateObservable = peer.getDownloadRateObservable();
        BindingUtils.subscribe(downloadRateObservable.map(DataSize::bestFitBytes).map(DataSize::toRateString),
                downSpeed, disposables);

        Observable<Double> uploadRateObservable = peer.getUploadRateObservable();
        BindingUtils.subscribe(uploadRateObservable.map(DataSize::bestFitBytes).map(DataSize::toRateString),
                upSpeed, disposables);

        return new UiPeer(ip, port, client, downSpeed, upSpeed, disposables);
    }

    public ReadOnlyStringProperty ipProperty() {
        return ip.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty portProperty() {
        return port.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty clientProperty() {
        return client.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty downSpeedProperty() {
        return downSpeed.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty upSpeedProperty() {
        return upSpeed.getReadOnlyProperty();
    }

    public void dispose() {
        disposables.dispose();
    }
}
