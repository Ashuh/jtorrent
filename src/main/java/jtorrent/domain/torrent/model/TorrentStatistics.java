package jtorrent.domain.torrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNegative;

import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class TorrentStatistics {

    private final AtomicLong downloaded;
    private final BehaviorSubject<Long> downloadedSubject;

    private final AtomicLong uploaded;
    private final BehaviorSubject<Long> uploadedSubject;

    public TorrentStatistics(long downloaded, long uploaded) {
        requireNonNegative(downloaded);
        requireNonNegative(uploaded);

        this.downloaded = new AtomicLong(downloaded);
        this.downloadedSubject = BehaviorSubject.createDefault(downloaded);

        this.uploaded = new AtomicLong(uploaded);
        this.uploadedSubject = BehaviorSubject.createDefault(uploaded);
    }

    public static TorrentStatistics createNew() {
        return new TorrentStatistics(0, 0);
    }

    public void incrementDownloaded(long bytes) {
        downloadedSubject.onNext(downloaded.addAndGet(bytes));
    }

    public void incrementUploaded(int bytes) {
        uploadedSubject.onNext(uploaded.addAndGet(bytes));
    }

    public long getDownloaded() {
        return downloaded.get();
    }

    public Observable<Long> getDownloadedObservable() {
        return downloadedSubject;
    }

    public long getUploaded() {
        return uploaded.get();
    }

    public Observable<Long> getUploadedObservable() {
        return uploadedSubject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TorrentStatistics that = (TorrentStatistics) o;
        return (downloaded.get() == that.downloaded.get()) && (uploaded.get() == that.uploaded.get());
    }

    @Override
    public int hashCode() {
        int result = downloaded.hashCode();
        result = 31 * result + uploaded.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TorrentStatistics{"
                + "uploaded=" + uploaded
                + ", downloaded=" + downloaded
                + '}';
    }
}
