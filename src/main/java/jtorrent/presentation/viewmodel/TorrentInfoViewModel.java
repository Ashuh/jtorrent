package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.time.LocalDateTime;
import java.util.BitSet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import jtorrent.domain.Client;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.util.BindingUtils;
import jtorrent.presentation.util.CalculateEtaCombiner;
import jtorrent.presentation.util.DataSize;
import jtorrent.presentation.util.DateFormatter;

public class TorrentInfoViewModel {

    private final Client client;
    private final ReadOnlyObjectWrapper<BitSet> downloadedPieces = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyStringWrapper downloadedPercentage = new ReadOnlyStringWrapper();
    private final ReadOnlyObjectWrapper<BitSet> availablePieces = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyStringWrapper availablePercentage = new ReadOnlyStringWrapper();
    private final ReadOnlyIntegerWrapper totalPieces = new ReadOnlyIntegerWrapper();
    private final ReadOnlyStringWrapper timeElapsed = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper remaining = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper wasted = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper downloaded = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper uploaded = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper seeds = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper downloadSpeed = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper uploadSpeed = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper peers = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper downLimit = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper upLimit = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper shareRatio = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper saveAs = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper pieces = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper totalSize = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper createdBy = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper createdOn = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper completedOn = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper hash = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper comment = new ReadOnlyStringWrapper();
    private CompositeDisposable disposables;

    public TorrentInfoViewModel(Client client) {
        this.client = requireNonNull(client);
    }

    public void setSelectedTorrent(Torrent torrent) {
        if (disposables != null) {
            disposables.dispose();
            disposables = null;
        }

        if (torrent == null) {
            reset();
            return;
        }

        Platform.runLater(() -> {
            totalPieces.set(torrent.getNumPieces());
            saveAs.set(torrent.getSaveAsPath().toString());
            createdBy.set(torrent.getCreatedBy());
            createdOn.set(formatDate(torrent.getCreationDate()));
            hash.set(torrent.getInfoHash().toString());
            comment.set(torrent.getComment());
        });

        disposables = new CompositeDisposable();

        BindingUtils.subscribe(torrent.getVerifiedPiecesObservable(), downloadedPieces, disposables);

        Observable<String> downloadedPercentageObservable = torrent.getVerifiedPiecesObservable()
                .map(BitSet::cardinality)
                .map(downloadedSize -> formatPercentage(downloadedSize, torrent.getNumPieces()));
        BindingUtils.subscribe(downloadedPercentageObservable, downloadedPercentage, disposables);

        BindingUtils.subscribe(torrent.getAvailablePiecesObservable(), availablePieces, disposables);

        Observable<String> availablePercentageObservable = Observable.just(""); // placeholder
        BindingUtils.subscribe(availablePercentageObservable, availablePercentage, disposables);

        Observable<String> remainingObservable = Observable.combineLatest(
                torrent.getVerifiedBytesObservable(),
                torrent.getDownloadRateObservable(),
                new CalculateEtaCombiner(torrent.getTotalSize())
        ).map(Object::toString);
        BindingUtils.subscribe(remainingObservable, remaining, disposables);

        Observable<String> downloadedObservable = torrent.getDownloadedObservable()
                .map(DataSize::bestFitBytes)
                .map(DataSize::toString);
        BindingUtils.subscribe(downloadedObservable, downloaded, disposables);

        Observable<String> uploadedObservable = torrent.getUploadedObservable()
                .map(DataSize::bestFitBytes)
                .map(DataSize::toString);
        BindingUtils.subscribe(uploadedObservable, uploaded, disposables);

        Observable<String> downloadRateObservable = torrent.getDownloadRateObservable()
                .map(DataSize::bestFitBytes)
                .map(DataSize::toRateString);
        BindingUtils.subscribe(downloadRateObservable, downloadSpeed, disposables);

        Observable<String> uploadRateObservable = torrent.getUploadRateObservable()
                .map(DataSize::bestFitBytes)
                .map(DataSize::toRateString);
        BindingUtils.subscribe(uploadRateObservable, uploadSpeed, disposables);

        Observable<String> piecesObservable = torrent.getVerifiedPiecesObservable()
                .map(BitSet::cardinality)
                .map(numVerified -> formatPieces(torrent.getNumPieces(), torrent.getPieceSize(), numVerified));
        BindingUtils.subscribe(piecesObservable, pieces, disposables);

        Observable<String> totalSizeObservable = torrent.getVerifiedBytesObservable()
                .map(verifiedBytes -> formatTotalSize(torrent.getTotalSize(), verifiedBytes));

        BindingUtils.subscribe(totalSizeObservable, totalSize, disposables);
    }

    private static String formatDate(LocalDateTime localDateTime) {
        return DateFormatter.format(localDateTime);
    }

    private void reset() {
        Platform.runLater(() -> {
            downloadedPieces.set(null);
            downloadedPercentage.set(null);
            availablePieces.set(null);
            availablePercentage.set(null);
            totalPieces.set(0);
            timeElapsed.set(null);
            remaining.set(null);
            wasted.set(null);
            downloaded.set(null);
            uploaded.set(null);
            seeds.set(null);
            downloadSpeed.set(null);
            uploadSpeed.set(null);
            peers.set(null);
            downLimit.set(null);
            upLimit.set(null);
            shareRatio.set(null);
            status.set(null);
            saveAs.set(null);
            pieces.set(null);
            totalSize.set(null);
            createdBy.set(null);
            createdOn.set(null);
            completedOn.set(null);
            hash.set(null);
            comment.set(null);
        });
    }

    private static String formatTotalSize(long totalSize, long verified) {
        return DataSize.bestFitBytes(totalSize) + " (" + DataSize.bestFitBytes(verified) + " done)";
    }

    private static String formatPieces(int numPieces, long pieceSize, int numVerifiedPieces) {
        return numPieces + " x " + DataSize.bestFitBytes(pieceSize) + " (" + numVerifiedPieces + " done)";
    }

    private static String formatPercentage(long value, long total) {
        return String.format("%.1f%%", value / (double) total * 100);
    }

    public ReadOnlyObjectProperty<BitSet> downloadedPiecesProperty() {
        return downloadedPieces.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty downloadedPercentageProperty() {
        return downloadedPercentage.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<BitSet> availablePiecesProperty() {
        return availablePieces.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty availablePercentageProperty() {
        return availablePercentage.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty totalPiecesProperty() {
        return totalPieces.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty timeElapsedProperty() {
        return timeElapsed.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty remainingProperty() {
        return remaining.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty wastedProperty() {
        return wasted.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty downloadedProperty() {
        return downloaded.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty uploadedProperty() {
        return uploaded.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty seedsProperty() {
        return seeds.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty downloadSpeedProperty() {
        return downloadSpeed.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty uploadSpeedProperty() {
        return uploadSpeed.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty peersProperty() {
        return peers.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty downLimitProperty() {
        return downLimit.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty upLimitProperty() {
        return upLimit.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty shareRatioProperty() {
        return shareRatio.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty saveAsProperty() {
        return saveAs.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty piecesProperty() {
        return pieces.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty totalSizeProperty() {
        return totalSize.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty createdByProperty() {
        return createdBy.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty createdOnProperty() {
        return createdOn.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty completedOnProperty() {
        return completedOn.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty hashProperty() {
        return hash.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty commentProperty() {
        return comment.getReadOnlyProperty();
    }
}
