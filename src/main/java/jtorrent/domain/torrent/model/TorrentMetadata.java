package jtorrent.domain.torrent.model;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

public record TorrentMetadata(List<List<URI>> trackerTiers, LocalDateTime creationDate, String comment,
                              String createdBy, FileInfo fileInfo) {

    public TorrentMetadata {
        if (trackerTiers.isEmpty() || trackerTiers.get(0).isEmpty()) {
            throw new IllegalArgumentException("At least one tracker is required");
        }
    }
}
