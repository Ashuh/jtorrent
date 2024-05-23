package jtorrent.data.torrent.source.db.model;

import jakarta.persistence.Column;
import jtorrent.domain.torrent.model.TorrentStatistics;

public class TorrentStatisticsComponent {

    @Column(nullable = false)
    private final long downloaded;

    @Column(nullable = false)
    private final long uploaded;

    protected TorrentStatisticsComponent() {
        this(0, 0);
    }

    public TorrentStatisticsComponent(long downloaded, long uploaded) {
        this.downloaded = downloaded;
        this.uploaded = uploaded;
    }

    public static TorrentStatisticsComponent fromDomain(TorrentStatistics torrentStatistics) {
        return new TorrentStatisticsComponent(torrentStatistics.getDownloaded(), torrentStatistics.getUploaded());
    }

    public TorrentStatistics toDomain() {
        return new TorrentStatistics(downloaded, uploaded);
    }

    public long getDownloaded() {
        return downloaded;
    }

    public long getUploaded() {
        return uploaded;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(downloaded);
        result = 31 * result + Long.hashCode(uploaded);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TorrentStatisticsComponent that = (TorrentStatisticsComponent) o;
        return downloaded == that.downloaded && uploaded == that.uploaded;
    }

    @Override
    public String toString() {
        return "TorrentStatisticsComponent{"
                + "downloaded=" + downloaded
                + ", uploaded=" + uploaded
                + '}';
    }
}
