package jtorrent.data.torrent.source.db.model;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.OrderColumn;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.TorrentMetadata;

@Embeddable
public class TorrentMetadataComponent {

    private static final String TRACKER_SEPARATOR = " ";

    /**
     * The list of tracker tiers. Each tier is a String made up of tracker URLs separated by a space.
     * This is a workaround to the fact that JPA does not support nested collections.
     */
    @OrderColumn
    @ElementCollection
    private final List<String> trackers;

    @Column(nullable = false)
    private final LocalDateTime creationDate;

    @Column(nullable = false)
    private final String comment;

    @Column(nullable = false)
    private final String createdBy;

    @Embedded
    private final FileInfoComponent fileInfo;

    protected TorrentMetadataComponent() {
        this(Collections.emptyList(), LocalDateTime.MIN, "", "", new FileInfoComponent());
    }

    public TorrentMetadataComponent(List<String> trackers, LocalDateTime creationDate, String comment, String createdBy,
            FileInfoComponent fileInfo) {
        this.trackers = trackers;
        this.creationDate = creationDate;
        this.comment = comment;
        this.createdBy = createdBy;
        this.fileInfo = fileInfo;
    }

    public static TorrentMetadataComponent fromDomain(TorrentMetadata torrentMetadata) {
        List<String> trackerTiers = torrentMetadata.trackerTiers().stream()
                .map(tier -> tier.stream()
                        .map(URI::toString)
                        .collect(Collectors.joining(TRACKER_SEPARATOR))
                )
                .toList();
        LocalDateTime creationDate = torrentMetadata.creationDate();
        String comment = torrentMetadata.comment();
        String createdBy = torrentMetadata.createdBy();
        FileInfoComponent fileInfo = FileInfoComponent.fromDomain(torrentMetadata.fileInfo());
        return new TorrentMetadataComponent(trackerTiers, creationDate, comment, createdBy, fileInfo);
    }

    public TorrentMetadata toDomain() {
        List<List<URI>> domainTrackers = trackers.stream()
                .map(tier -> Arrays.stream(tier.split(TRACKER_SEPARATOR))
                        .map(URI::create)
                        .toList()
                )
                .toList();
        FileInfo domainFileInfo = fileInfo.toDomain();
        return new TorrentMetadata(domainTrackers, creationDate, comment, createdBy, domainFileInfo);
    }

    public List<String> getTrackers() {
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
