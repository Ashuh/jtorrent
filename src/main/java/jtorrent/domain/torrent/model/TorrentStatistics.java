package jtorrent.domain.torrent.model;

import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class TorrentStatistics {

    private final AtomicLong downloaded = new AtomicLong(0);
    private final BehaviorSubject<Long> downloadedSubject = BehaviorSubject.createDefault(0L);

    private final AtomicLong uploaded = new AtomicLong(0);
    private final BehaviorSubject<Long> uploadedSubject = BehaviorSubject.createDefault(0L);

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
}
