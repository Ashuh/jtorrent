package jtorrent.domain.torrent.model;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class TorrentStatistics {

    private final AtomicInteger downloaded = new AtomicInteger(0);
    private final BehaviorSubject<Integer> downloadedSubject = BehaviorSubject.createDefault(0);

    private final AtomicInteger uploaded = new AtomicInteger(0);
    private final BehaviorSubject<Integer> uploadedSubject = BehaviorSubject.createDefault(0);

    public void incrementDownloaded(int bytes) {
        downloadedSubject.onNext(downloaded.addAndGet(bytes));
    }

    public void incrementUploaded(int bytes) {
        uploadedSubject.onNext(uploaded.addAndGet(bytes));
    }

    public long getDownloaded() {
        return downloaded.get();
    }

    public Observable<Integer> getDownloadedObservable() {
        return downloadedSubject;
    }

    public long getUploaded() {
        return uploaded.get();
    }

    public Observable<Integer> getUploadedObservable() {
        return uploadedSubject;
    }
}
