package jtorrent.data.torrent.source.db.model;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.TorrentMetadata;

@Embeddable
public class TorrentMetadataComponent {

    @ElementCollection
    private final Set<String> trackers;

    @Column(nullable = false)
    private final LocalDateTime creationDate;

    @Column(nullable = false)
    private final String comment;

    @Column(nullable = false)
    private final String createdBy;

    @Embedded
    private final FileInfoComponent fileInfo;

    protected TorrentMetadataComponent() {
        this(Collections.emptySet(), LocalDateTime.MIN, "", "", new FileInfoComponent());
    }

    public TorrentMetadataComponent(Set<String> trackers, LocalDateTime creationDate, String comment, String createdBy,
            FileInfoComponent fileInfo) {
        this.trackers = trackers;
        this.creationDate = creationDate;
        this.comment = comment;
        this.createdBy = createdBy;
        this.fileInfo = fileInfo;
    }

    public static TorrentMetadataComponent fromDomain(TorrentMetadata torrentMetadata) {
        Set<String> trackers = torrentMetadata.trackers().stream()
                .map(URI::toString)
                .collect(Collectors.toSet());
        LocalDateTime creationDate = torrentMetadata.creationDate();
        String comment = torrentMetadata.comment();
        String createdBy = torrentMetadata.createdBy();
        FileInfoComponent fileInfo = FileInfoComponent.fromDomain(torrentMetadata.fileInfo());
        return new TorrentMetadataComponent(trackers, creationDate, comment, createdBy, fileInfo);
    }

    public TorrentMetadata toDomain() {
        Set<URI> domainTrackers = trackers.stream()
                .map(URI::create)
                .collect(Collectors.toSet());
        FileInfo domainFileInfo = fileInfo.toDomain();
        return new TorrentMetadata(domainTrackers, creationDate, comment, createdBy, domainFileInfo);
    }

    public Set<String> getTrackers() {
        return trackers;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public FileInfoComponent getFileInfo() {
        return fileInfo;
    }

    @Override
    public int hashCode() {
        int result = trackers.hashCode();
        result = 31 * result + creationDate.hashCode();
        result = 31 * result + comment.hashCode();
        result = 31 * result + createdBy.hashCode();
        result = 31 * result + fileInfo.hashCode();
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

        TorrentMetadataComponent that = (TorrentMetadataComponent) o;
        return trackers.equals(that.trackers)
                && creationDate.equals(that.creationDate)
                && comment.equals(that.comment)
                && createdBy.equals(that.createdBy)
                && fileInfo.equals(that.fileInfo);
    }

    @Override
    public String toString() {
        return "TorrentMetadataComponent{"
                + "trackers=" + trackers
                + ", creationDate=" + creationDate
                + ", comment='" + comment + '\''
                + ", createdBy='" + createdBy + '\''
                + ", fileInfo=" + fileInfo
                + '}';
    }
}
