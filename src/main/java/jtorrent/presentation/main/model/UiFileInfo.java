package jtorrent.presentation.main.model;

import java.util.BitSet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.FileMetadataWithState;
import jtorrent.domain.torrent.model.FileProgress;
import jtorrent.presentation.common.util.BindingUtils;
import jtorrent.presentation.common.util.DataSize;

public class UiFileInfo {

    private final ReadOnlyStringWrapper path;
    private final ReadOnlyStringWrapper size;
    private final ReadOnlyStringWrapper done;
    private final ReadOnlyStringWrapper percentDone;
    private final ReadOnlyIntegerWrapper firstPiece;
    private final ReadOnlyIntegerWrapper numPieces;
    private final ReadOnlyObjectWrapper<BitSet> downloadedPieces;
    private final ReadOnlyStringWrapper priority;
    private final ReadOnlyStringWrapper mode;
    private final ReadOnlyStringWrapper rate;
    private final ReadOnlyStringWrapper resolution;
    private final ReadOnlyStringWrapper duration;
    private final ReadOnlyStringWrapper streamable;
    private final ReadOnlyStringWrapper hasHeader;
    private final ReadOnlyStringWrapper codecs;
    private final CompositeDisposable disposables;

    private UiFileInfo(ReadOnlyStringWrapper path, ReadOnlyStringWrapper size, ReadOnlyStringWrapper done,
            ReadOnlyStringWrapper percentDone, ReadOnlyIntegerWrapper firstPiece, ReadOnlyIntegerWrapper numPieces,
            ReadOnlyObjectWrapper<BitSet> downloadedPieces, ReadOnlyStringWrapper priority, ReadOnlyStringWrapper mode,
            ReadOnlyStringWrapper rate, ReadOnlyStringWrapper resolution, ReadOnlyStringWrapper duration,
            ReadOnlyStringWrapper streamable, ReadOnlyStringWrapper hasHeader, ReadOnlyStringWrapper codecs,
            CompositeDisposable disposables) {
        this.path = path;
        this.size = size;
        this.done = done;
        this.percentDone = percentDone;
        this.firstPiece = firstPiece;
        this.numPieces = numPieces;
        this.downloadedPieces = downloadedPieces;
        this.priority = priority;
        this.mode = mode;
        this.rate = rate;
        this.resolution = resolution;
        this.duration = duration;
        this.streamable = streamable;
        this.hasHeader = hasHeader;
        this.codecs = codecs;
        this.disposables = disposables;
    }

    public static UiFileInfo fromDomain(FileMetadataWithState fileMetadataWithState) {
        FileMetadata fileMetadata = fileMetadataWithState.fileMetaData();
        FileProgress fileProgress = fileMetadataWithState.fileProgress();

        ReadOnlyStringWrapper path = new ReadOnlyStringWrapper(fileMetadata.path().toString());
        ReadOnlyStringWrapper size = new ReadOnlyStringWrapper(DataSize.bestFitBytes(fileMetadata.size()).toString());
        ReadOnlyStringWrapper done = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper percentDone = new ReadOnlyStringWrapper("");
        ReadOnlyIntegerWrapper firstPiece = new ReadOnlyIntegerWrapper(fileMetadata.firstPiece());
        ReadOnlyIntegerWrapper numPieces = new ReadOnlyIntegerWrapper(fileMetadata.numPieces());
        ReadOnlyObjectWrapper<BitSet> pieces = new ReadOnlyObjectWrapper<>(new BitSet());
        ReadOnlyStringWrapper priority = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper mode = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper rate = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper resolution = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper duration = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper streamable = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper hasHeader = new ReadOnlyStringWrapper("");
        ReadOnlyStringWrapper codecs = new ReadOnlyStringWrapper("");
        CompositeDisposable disposables = new CompositeDisposable();

        Observable<String> doneObservable = fileProgress.getVerifiedBytesObservable()
                .map(DataSize::bestFitBytes)
                .map(DataSize::toString);
        BindingUtils.subscribe(doneObservable, done, disposables);

        Observable<String> percentDoneObservable = fileProgress.getVerifiedBytesObservable()
                .map(verifiedBytes -> (double) verifiedBytes / fileMetadata.size())
                .map(percent -> String.format("%.1f%%", percent * 100));
        BindingUtils.subscribe(percentDoneObservable, percentDone, disposables);

        Observable<BitSet> fileVerifiedPiecesObservable = fileProgress.getVerifiedPiecesObservable();
        BindingUtils.subscribe(fileVerifiedPiecesObservable, pieces, disposables);

        return new UiFileInfo(path, size, done, percentDone, firstPiece, numPieces, pieces, priority, mode,
                rate, resolution, duration, streamable, hasHeader, codecs, disposables);
    }

    public ReadOnlyStringProperty pathProperty() {
        return path.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty sizeProperty() {
        return size.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty doneProperty() {
        return done.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty percentDoneProperty() {
        return percentDone.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty firstPieceProperty() {
        return firstPiece.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty numPiecesProperty() {
        return numPieces.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<BitSet> downloadedPiecesProperty() {
        return downloadedPieces.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty priorityProperty() {
        return priority.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty modeProperty() {
        return mode.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty rateProperty() {
        return rate.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty resolutionProperty() {
        return resolution.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty durationProperty() {
        return duration.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty streamableProperty() {
        return streamable.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty hasHeaderProperty() {
        return hasHeader.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty codecsProperty() {
        return codecs.getReadOnlyProperty();
    }

    public void dispose() {
        disposables.dispose();
    }
}
