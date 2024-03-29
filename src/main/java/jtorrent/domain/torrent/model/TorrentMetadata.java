package jtorrent.domain.torrent.model;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Set;

public record TorrentMetadata(Set<URI> trackers, LocalDateTime creationDate, String comment, String createdBy,
                              FileInfo fileInfo) {
}
