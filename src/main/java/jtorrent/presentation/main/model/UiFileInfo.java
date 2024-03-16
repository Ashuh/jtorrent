package jtorrent.presentation.main.model;

import java.util.BitSet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jtorrent.domain.torrent.model.File;
import jtorrent.domain.torrent.model.FilePieceInfo;
import jtorrent.domain.torrent.model.FileWithPieceInfo;
import jtorrent.presentation.common.util.BindingUtils;
import jtorrent.presentation.common.util.DataSize;

public class UiFileInfo {

    private final StringProperty path;
    private final StringProperty size;
    private final StringProperty done;
    private final StringProperty percentDone;
    private final IntegerProperty firstPiece;
    private final IntegerProperty numPieces;
    private final ObjectProperty<BitSet> downloadedPieces;
    private final StringProperty priority;
    private final StringProperty mode;
    private final StringProperty rate;
    private final StringProperty resolution;
    private final StringProperty duration;
    private final StringProperty streamable;
    private final StringProperty hasHeader;
    private final StringProperty codecs;
    private final CompositeDisposable disposables;

    private UiFileInfo(StringProperty path, StringProperty size, StringProperty done, StringProperty percentDone,
            IntegerProperty firstPiece, IntegerProperty numPieces, ObjectProperty<BitSet> downloadedPieces,
            StringProperty priority, StringProperty mode, StringProperty rate, StringProperty resolution,
            StringProperty duration, StringProperty streamable, StringProperty hasHeader, StringProperty codecs,
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

    public static UiFileInfo fromDomain(FileWithPieceInfo fileWithPieceInfo) {
        File file = fileWithPieceInfo.file();
        FilePieceInfo filePieceInfo = fileWithPieceInfo.filePieceInfo();

        StringProperty path = new SimpleStringProperty(file.getPath().toString());
        StringProperty size = new SimpleStringProperty(DataSize.bestFitBytes(file.getSize()).toString());
        StringProperty done = new SimpleStringProperty("");
        StringProperty percentDone = new SimpleStringProperty("");
        IntegerProperty firstPiece = new SimpleIntegerProperty(filePieceInfo.firstPiece());
        IntegerProperty numPieces = new SimpleIntegerProperty(filePieceInfo.numPieces());
        ObjectProperty<BitSet> pieces = new SimpleObjectProperty<>(new BitSet());
        StringProperty priority = new SimpleStringProperty("");
        StringProperty mode = new SimpleStringProperty("");
        StringProperty rate = new SimpleStringProperty("");
        StringProperty resolution = new SimpleStringProperty("");
        StringProperty duration = new SimpleStringProperty("");
        StringProperty streamable = new SimpleStringProperty("");
        StringProperty hasHeader = new SimpleStringProperty("");
        StringProperty codecs = new SimpleStringProperty("");
        CompositeDisposable disposables = new CompositeDisposable();

        Observable<String> doneObservable = filePieceInfo.getVerifiedBytesObservable()
                .map(DataSize::bestFitBytes)
                .map(DataSize::toString);
        BindingUtils.subscribe(doneObservable, done, disposables);

        Observable<String> percentDoneObservable = filePieceInfo.getVerifiedBytesObservable()
                .map(verifiedBytes -> (double) verifiedBytes / file.getSize())
                .map(percent -> String.format("%.1f%%", percent * 100));
        BindingUtils.subscribe(percentDoneObservable, percentDone, disposables);

        Observable<BitSet> fileVerifiedPiecesObservable = filePieceInfo.getVerifiedPiecesObservable();
        BindingUtils.subscribe(fileVerifiedPiecesObservable, pieces, disposables);

        return new UiFileInfo(path, size, done, percentDone, firstPiece, numPieces, pieces, priority, mode,
                rate, resolution, duration, streamable, hasHeader, codecs, disposables);
    }

    public String getPath() {
        return path.get();
    }

    public StringProperty pathProperty() {
        return path;
    }

    public void setPath(String path) {
        this.path.set(path);
    }

    public String getSize() {
        return size.get();
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public void setSize(String size) {
        this.size.set(size);
    }

    public String getDone() {
        return done.get();
    }

    public StringProperty doneProperty() {
        return done;
    }

    public void setDone(String done) {
        this.done.set(done);
    }

    public String getPercentDone() {
        return percentDone.get();
    }

    public StringProperty percentDoneProperty() {
        return percentDone;
    }

    public void setPercentDone(String percentDone) {
        this.percentDone.set(percentDone);
    }

    public int getFirstPiece() {
        return firstPiece.get();
    }

    public IntegerProperty firstPieceProperty() {
        return firstPiece;
    }

    public void setFirstPiece(int firstPiece) {
        this.firstPiece.set(firstPiece);
    }

    public int getNumPieces() {
        return numPieces.get();
    }

    public IntegerProperty numPiecesProperty() {
        return numPieces;
    }

    public void setNumPieces(int numPieces) {
        this.numPieces.set(numPieces);
    }

    public BitSet getDownloadedPieces() {
        return downloadedPieces.get();
    }

    public ObjectProperty<BitSet> downloadedPiecesProperty() {
        return downloadedPieces;
    }

    public void setDownloadedPieces(BitSet downloadedPieces) {
        this.downloadedPieces.set(downloadedPieces);
    }

    public String getPriority() {
        return priority.get();
    }

    public StringProperty priorityProperty() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority.set(priority);
    }

    public String getMode() {
        return mode.get();
    }

    public StringProperty modeProperty() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode.set(mode);
    }

    public String getRate() {
        return rate.get();
    }

    public StringProperty rateProperty() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate.set(rate);
    }

    public String getResolution() {
        return resolution.get();
    }

    public StringProperty resolutionProperty() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution.set(resolution);
    }

    public String getDuration() {
        return duration.get();
    }

    public StringProperty durationProperty() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration.set(duration);
    }

    public String getStreamable() {
        return streamable.get();
    }

    public StringProperty streamableProperty() {
        return streamable;
    }

    public void setStreamable(String streamable) {
        this.streamable.set(streamable);
    }

    public String getHasHeader() {
        return hasHeader.get();
    }

    public StringProperty hasHeaderProperty() {
        return hasHeader;
    }

    public void setHasHeader(String hasHeader) {
        this.hasHeader.set(hasHeader);
    }

    public String getCodecs() {
        return codecs.get();
    }

    public StringProperty codecsProperty() {
        return codecs;
    }

    public void setCodecs(String codecs) {
        this.codecs.set(codecs);
    }

    public void dispose() {
        disposables.dispose();
    }
}
