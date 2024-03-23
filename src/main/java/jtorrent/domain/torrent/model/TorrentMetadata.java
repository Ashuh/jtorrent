package jtorrent.domain.torrent.model;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Set;

import jtorrent.domain.common.util.Sha1Hash;

public record TorrentMetadata(Set<URI> trackers, LocalDateTime creationDate, String comment, String createdBy,
                              FileInfo fileInfo, Sha1Hash infoHash) {
}
