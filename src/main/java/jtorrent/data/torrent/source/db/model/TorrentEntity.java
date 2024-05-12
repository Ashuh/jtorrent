package jtorrent.data.torrent.source.db.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.domain.torrent.model.TorrentProgress;
import jtorrent.domain.torrent.model.TorrentStatistics;

@Entity
public class TorrentEntity {

    @Column(nullable = false)
    private final String displayName;
    @Column(nullable = false)
    private final String saveDirectory;
    @Embedded
    private final TorrentMetadataComponent metadata;
    @Embedded
    private final TorrentStatisticsComponent statistics;
    @Embedded
    private final TorrentProgressComponent progress;
    @Enumerated
    @Column(nullable = false)
    private final Torrent.State state;
    @Id
    @Column(length = 20)
    private byte[] infoHash;

    protected TorrentEntity() {
        this(new byte[0], "", "", new TorrentMetadataComponent(), new TorrentStatisticsComponent(),
                new TorrentProgressComponent(), Torrent.State.STOPPED);
    }

    public TorrentEntity(byte[] infoHash, String displayName, String saveDirectory, TorrentMetadataComponent metadata,
            TorrentStatisticsComponent statistics, TorrentProgressComponent progress, Torrent.State state) {
        this.infoHash = infoHash;
        this.displayName = displayName;
        this.saveDirectory = saveDirectory;
        this.metadata = metadata;
        this.statistics = statistics;
        this.progress = progress;
        this.state = state;
    }

    public static TorrentEntity fromDomain(Torrent torrent) {
        return new TorrentEntity(
                torrent.getInfoHash().getBytes(),
                torrent.getName(),
                torrent.getSaveDirectory().toString(),
                TorrentMetadataComponent.fromDomain(torrent.getMetadata()),
                TorrentStatisticsComponent.fromDomain(torrent.getStatistics()),
                TorrentProgressComponent.fromDomain(torrent.getProgress()),
                torrent.getState()
        );
    }

    public Torrent toDomain() {
        TorrentMetadata domainMetadata = metadata.toDomain();
        TorrentStatistics domainStatistics = statistics.toDomain();
        TorrentProgress domainProgress = progress.toDomain(domainMetadata.fileInfo());
        Path domainSaveDirectory = Paths.get(saveDirectory);
        return new Torrent(domainMetadata, domainStatistics, domainProgress, displayName, domainSaveDirectory, state);
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSaveDirectory() {
        return saveDirectory;
    }

    public TorrentMetadataComponent getMetadata() {
        return metadata;
    }

    public TorrentStatisticsComponent getStatistics() {
        return statistics;
    }

    public TorrentProgressComponent getProgress() {
        return progress;
    }

    public Torrent.State getState() {
        return state;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(infoHash);
        result = 31 * result + displayName.hashCode();
        result = 31 * result + saveDirectory.hashCode();
        result = 31 * result + metadata.hashCode();
        result = 31 * result + statistics.hashCode();
        result = 31 * result + progress.hashCode();
        result = 31 * result + state.hashCode();
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

        TorrentEntity that = (TorrentEntity) o;
        return Arrays.equals(infoHash, that.infoHash)
                && displayName.equals(that.displayName)
                && saveDirectory.equals(that.saveDirectory)
                && metadata.equals(that.metadata)
                && statistics.equals(that.statistics)
                && progress.equals(that.progress)
                && state == that.state;
    }

    @Override
    public String toString() {
        return "TorrentEntity{"
                + "infoHash=" + Arrays.toString(infoHash)
                + ", displayName='" + displayName + '\''
                + ", saveDirectory='" + saveDirectory + '\''
                + ", metadata=" + metadata
                + ", statistics=" + statistics
                + ", progress=" + progress
                + '}';
    }
}
