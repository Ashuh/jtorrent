package jtorrent.torrent.presentation;

import java.time.LocalDateTime;
import java.util.BitSet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.common.presentation.util.BindingUtils;
import jtorrent.common.presentation.util.DataUnitFormatter;
import jtorrent.common.presentation.util.DateFormatter;
import jtorrent.torrent.domain.model.Torrent;

public class UiTorrentInfo {

    private final ObjectProperty<BitSet> downloadedPieces;
    private final StringProperty downloadedPercentage;
    private final ObjectProperty<BitSet> availablePieces;
    private final StringProperty availablePercentage;
    private final IntegerProperty totalPieces;
    private final StringProperty timeElapsed;
    private final StringProperty remaining;
    private final StringProperty wasted;
    private final StringProperty downloaded;
    private final StringProperty uploaded;
    private final StringProperty seeds;
    private final StringProperty downloadSpeed;
    private final StringProperty uploadSpeed;
    private final StringProperty peers;
    private final StringProperty downLimit;
    private final StringProperty upLimit;
    private final StringProperty shareRatio;
    private final StringProperty status;
    private final StringProperty saveAs;
    private final StringProperty pieces;
    private final StringProperty totalSize;
    private final StringProperty createdBy;
    private final StringProperty createdOn;
    private final StringProperty completedOn;
    private final StringProperty hash;
    private final StringProperty comment;
    private final CompositeDisposable disposables;

    private UiTorrentInfo(ObjectProperty<BitSet> downloadedPieces, StringProperty downloadedPercentage,
            ObjectProperty<BitSet> availablePieces, StringProperty availablePercentage, IntegerProperty totalPieces,
            StringProperty timeElapsed, StringProperty remaining, StringProperty wasted, StringProperty downloaded,
            StringProperty uploaded, StringProperty seeds, StringProperty downloadSpeed, StringProperty uploadSpeed,
            StringProperty peers, StringProperty downLimit, StringProperty upLimit, StringProperty shareRatio,
            StringProperty status, StringProperty saveAs, StringProperty pieces, StringProperty totalSize,
            StringProperty createdBy, StringProperty createdOn, StringProperty completedOn, StringProperty hash,
            StringProperty comment, CompositeDisposable disposables) {
        this.downloadedPieces = downloadedPieces;
        this.downloadedPercentage = downloadedPercentage;
        this.availablePieces = availablePieces;
        this.availablePercentage = availablePercentage;
        this.totalPieces = totalPieces;
        this.timeElapsed = timeElapsed;
        this.remaining = remaining;
        this.wasted = wasted;
        this.downloaded = downloaded;
        this.uploaded = uploaded;
        this.seeds = seeds;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.peers = peers;
        this.downLimit = downLimit;
        this.upLimit = upLimit;
        this.shareRatio = shareRatio;
        this.status = status;
        this.saveAs = saveAs;
        this.pieces = pieces;
        this.totalSize = totalSize;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.completedOn = completedOn;
        this.hash = hash;
        this.comment = comment;
        this.disposables = disposables;
    }

    public static UiTorrentInfo fromDomain(Torrent torrent) {
        ObjectProperty<BitSet> downloadedPieces = new SimpleObjectProperty<>();
        StringProperty downloadedPercentage = new SimpleStringProperty();
        ObjectProperty<BitSet> availablePieces = new SimpleObjectProperty<>();
        StringProperty availablePercentage = new SimpleStringProperty();
        IntegerProperty totalPieces = new SimpleIntegerProperty(torrent.getNumPieces());
        StringProperty timeElapsed = new SimpleStringProperty();
        StringProperty remaining = new SimpleStringProperty();
        StringProperty wasted = new SimpleStringProperty();
        StringProperty downloaded = new SimpleStringProperty();
        StringProperty uploaded = new SimpleStringProperty();
        StringProperty seeds = new SimpleStringProperty();
        StringProperty downloadSpeed = new SimpleStringProperty();
        StringProperty uploadSpeed = new SimpleStringProperty();
        StringProperty peers = new SimpleStringProperty();
        StringProperty downLimit = new SimpleStringProperty();
        StringProperty upLimit = new SimpleStringProperty();
        StringProperty shareRatio = new SimpleStringProperty();
        StringProperty status = new SimpleStringProperty();
        StringProperty saveAs = new SimpleStringProperty();
        StringProperty pieces = new SimpleStringProperty(
                formatPieces(
                        torrent.getNumPieces(),
                        torrent.getPieceSize(),
                        torrent.getVerifiedPieces().cardinality()
                )
        );
        StringProperty totalSize = new SimpleStringProperty(formatTotalSize(torrent.getTotalSize(), 0));
        StringProperty createdBy = new SimpleStringProperty(torrent.getCreatedBy());
        StringProperty createdOn = new SimpleStringProperty(formatDate(torrent.getCreationDate()));
        StringProperty completedOn = new SimpleStringProperty();
        StringProperty hash = new SimpleStringProperty(torrent.getInfoHash().toString());
        StringProperty comment = new SimpleStringProperty(torrent.getComment());
        CompositeDisposable disposables = new CompositeDisposable();

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

        Observable<String> downloadedObservable = torrent.getDownloadedObservable().map(DataUnitFormatter::formatSize);
        BindingUtils.subscribe(downloadedObservable, downloaded, disposables);

        Observable<String> uploadedObservable = torrent.getUploadedObservable().map(DataUnitFormatter::formatSize);
        BindingUtils.subscribe(uploadedObservable, uploaded, disposables);

        Observable<String> downloadRateObservable = torrent.getDownloadRateObservable()
                .map(DataUnitFormatter::formatRate);
        BindingUtils.subscribe(downloadRateObservable, downloadSpeed, disposables);

        Observable<String> uploadRateObservable = torrent.getUploadRateObservable().map(DataUnitFormatter::formatRate);
        BindingUtils.subscribe(uploadRateObservable, uploadSpeed, disposables);

        Observable<String> piecesObservable = torrent.getVerifiedPiecesObservable()
                .map(BitSet::cardinality)
                .map(numVerified -> formatPieces(torrent.getNumPieces(), torrent.getPieceSize(), numVerified));
        BindingUtils.subscribe(piecesObservable, pieces, disposables);

        Observable<String> totalSizeObservable = torrent.getVerifiedBytesObservable()
                .map(verifiedBytes -> formatTotalSize(torrent.getTotalSize(), verifiedBytes));
        BindingUtils.subscribe(totalSizeObservable, totalSize, disposables);

        return new UiTorrentInfo(downloadedPieces, downloadedPercentage, availablePieces, availablePercentage,
                totalPieces, timeElapsed, remaining, wasted, downloaded, uploaded, seeds, downloadSpeed, uploadSpeed,
                peers, downLimit, upLimit, shareRatio, status, saveAs, pieces, totalSize, createdBy, createdOn,
                completedOn, hash, comment, disposables);
    }

    private static String formatTotalSize(long totalSize, long verified) {
        return DataUnitFormatter.formatSize(totalSize) + " (" + DataUnitFormatter.formatSize(verified) + " done)";
    }

    private static String formatDate(LocalDateTime localDateTime) {
        return DateFormatter.format(localDateTime);
    }

    private static String formatPieces(int numPieces, long pieceSize, int numVerifiedPieces) {
        return numPieces + " x " + DataUnitFormatter.formatSize(pieceSize) + " (" + numVerifiedPieces + " done)";
    }

    private static String formatPercentage(long value, long total) {
        return String.format("%.1f%%", value / (double) total * 100);
    }

    public void dispose() {
        disposables.dispose();
    }

    public BitSet getDownloadedPieces() {
        return downloadedPieces.get();
    }

    public ObjectProperty<BitSet> downloadedPiecesProperty() {
        return downloadedPieces;
    }

    public String getDownloadedPercentage() {
        return downloadedPercentage.get();
    }

    public StringProperty downloadedPercentageProperty() {
        return downloadedPercentage;
    }

    public BitSet getAvailablePieces() {
        return availablePieces.get();
    }

    public ObjectProperty<BitSet> availablePiecesProperty() {
        return availablePieces;
    }

    public String getAvailablePercentage() {
        return availablePercentage.get();
    }

    public StringProperty availablePercentageProperty() {
        return availablePercentage;
    }

    public int getTotalPieces() {
        return totalPieces.get();
    }

    public IntegerProperty totalPiecesProperty() {
        return totalPieces;
    }

    public String getTimeElapsed() {
        return timeElapsed.get();
    }

    public StringProperty timeElapsedProperty() {
        return timeElapsed;
    }

    public String getRemaining() {
        return remaining.get();
    }

    public StringProperty remainingProperty() {
        return remaining;
    }

    public String getWasted() {
        return wasted.get();
    }

    public StringProperty wastedProperty() {
        return wasted;
    }

    public String getDownloaded() {
        return downloaded.get();
    }

    public StringProperty downloadedProperty() {
        return downloaded;
    }

    public String getUploaded() {
        return uploaded.get();
    }

    public StringProperty uploadedProperty() {
        return uploaded;
    }

    public String getSeeds() {
        return seeds.get();
    }

    public StringProperty seedsProperty() {
        return seeds;
    }

    public String getDownloadSpeed() {
        return downloadSpeed.get();
    }

    public StringProperty downloadSpeedProperty() {
        return downloadSpeed;
    }

    public String getUploadSpeed() {
        return uploadSpeed.get();
    }

    public StringProperty uploadSpeedProperty() {
        return uploadSpeed;
    }

    public String getPeers() {
        return peers.get();
    }

    public StringProperty peersProperty() {
        return peers;
    }

    public String getDownLimit() {
        return downLimit.get();
    }

    public StringProperty downLimitProperty() {
        return downLimit;
    }

    public String getUpLimit() {
        return upLimit.get();
    }

    public StringProperty upLimitProperty() {
        return upLimit;
    }

    public String getShareRatio() {
        return shareRatio.get();
    }

    public StringProperty shareRatioProperty() {
        return shareRatio;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public String getSaveAs() {
        return saveAs.get();
    }

    public StringProperty saveAsProperty() {
        return saveAs;
    }

    public String getPieces() {
        return pieces.get();
    }

    public StringProperty piecesProperty() {
        return pieces;
    }

    public String getTotalSize() {
        return totalSize.get();
    }

    public StringProperty totalSizeProperty() {
        return totalSize;
    }

    public String getCreatedBy() {
        return createdBy.get();
    }

    public StringProperty createdByProperty() {
        return createdBy;
    }

    public String getCreatedOn() {
        return createdOn.get();
    }

    public StringProperty createdOnProperty() {
        return createdOn;
    }

    public String getCompletedOn() {
        return completedOn.get();
    }

    public StringProperty completedOnProperty() {
        return completedOn;
    }

    public String getHash() {
        return hash.get();
    }

    public StringProperty hashProperty() {
        return hash;
    }

    public String getComment() {
        return comment.get();
    }

    public StringProperty commentProperty() {
        return comment;
    }
}
