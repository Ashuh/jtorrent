package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import jtorrent.domain.Client;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.model.UiTorrentContents;
import jtorrent.presentation.util.BindingUtils;

public class TorrentControlsViewModel {

    private final Client client;
    private final ReadOnlyObjectWrapper<Optional<Torrent.State>> selectedTorrentState =
            new ReadOnlyObjectWrapper<>(Optional.empty());

    private Torrent selectedTorrent;
    private Disposable stateSubscription;

    public TorrentControlsViewModel(Client client) {
        this.client = requireNonNull(client);
    }

    public void setSelectedTorrent(Torrent torrent) {
        selectedTorrent = torrent;

        if (stateSubscription != null) {
            stateSubscription.dispose();
            stateSubscription = null;
        }

        if (selectedTorrent == null) {
            Platform.runLater(() -> selectedTorrentState.setValue(Optional.empty()));
            return;
        }

        Observable<Optional<Torrent.State>> stateObservable = torrent.getStateObservable().map(Optional::of);
        stateSubscription = BindingUtils.subscribe(stateObservable, selectedTorrentState);
    }

    public void startSelectedTorrent() {
        if (selectedTorrent == null) {
            return;
        }
        client.startTorrent(selectedTorrent);
    }

    public void stopSelectedTorrent() {
        if (selectedTorrent == null) {
            return;
        }
        client.stopTorrent(selectedTorrent);
    }

    public void removeSelectedTorrent() {
        if (selectedTorrent == null) {
            return;
        }
        client.removeTorrent(selectedTorrent);
    }

    public UiTorrentContents loadTorrentContents(String urlString) throws IOException {
        URL url = new URL(urlString);
        Torrent torrent = client.loadTorrent(url);
        return UiTorrentContents.forTorrent(torrent);
    }

    public UiTorrentContents loadTorrentContents(File file) throws IOException {
        Torrent torrent = client.loadTorrent(file);
        return UiTorrentContents.forTorrent(torrent);
    }

    public void addTorrent(UiTorrentContents torrentContents) {
        client.addTorrent(torrentContents.getTorrent());
    }

    public void createNewTorrent(File savePath, File source, String trackerUrls, String comment, int pieceSize)
            throws IOException {
        List<List<String>> trackerTiers = new ArrayList<>();

        for (String tier : trackerUrls.split("\n\n")) {
            List<String> trackers = Arrays.asList(tier.split("\n"));
            trackerTiers.add(trackers);
        }

        client.createNewTorrent(savePath.toPath(), source.toPath(), trackerTiers, comment, pieceSize);
    }

    public ReadOnlyObjectProperty<Optional<Torrent.State>> selectedTorrentStateProperty() {
        return selectedTorrentState.getReadOnlyProperty();
    }
}
