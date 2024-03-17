package jtorrent.data.torrent.model;

import static jtorrent.data.torrent.model.util.MapUtil.getValueAsList;
import static jtorrent.data.torrent.model.util.MapUtil.getValueAsLong;
import static jtorrent.data.torrent.model.util.MapUtil.getValueAsMap;
import static jtorrent.data.torrent.model.util.MapUtil.getValueAsString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.dampcake.bencode.BencodeInputStream;

import jtorrent.data.torrent.model.exception.MappingException;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.bencode.BencodedObject;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.tracker.model.Tracker;
import jtorrent.domain.tracker.model.factory.TrackerFactory;

public class BencodedTorrent extends BencodedObject {

    public static final String KEY_ANNOUNCE = "announce";
    public static final String KEY_ANNOUNCE_LIST = "announce-list";
    public static final String KEY_CREATION_DATE = "creation date";
    public static final String KEY_COMMENT = "comment";
    public static final String KEY_CREATED_BY = "created by";
    public static final String KEY_INFO = "info";

    private final String announce;
    /**
     * Optional field.
     * List of tiers, each containing a list of tracker URLs.
     * If this field is present, the announce field is ignored.
     *
     * @see <a href="https://www.bittorrent.org/beps/bep_0012.html">BEP 12 - Multitracker Metadata Extension</a>
     */
    private final List<List<String>> announceList;
    private final Long creationDate;
    private final String comment;
    private final String createdBy;
    private final BencodedInfo info;

    public BencodedTorrent(Long creationDate, String announce, List<List<String>> announceList,
            String comment, String createdBy, BencodedInfo info) {
        this.announce = announce;
        this.announceList = announceList;
        this.creationDate = creationDate;
        this.comment = comment;
        this.createdBy = createdBy;
        this.info = info;
    }

    public static BencodedTorrent decode(InputStream inputStream) throws IOException {
        BencodeInputStream bis = new BencodeInputStream(inputStream, StandardCharsets.UTF_8, true);
        Map<String, Object> topLevelDict = bis.readDictionary();
        return fromMap(topLevelDict);
    }

    /**
     * Create a new {@link BencodedTorrent} instance with the current time as the creation date.
     *
     * @param trackerUrls list of tiers, each containing a list of tracker URLs
     * @param comment     comment about the torrent
     * @param createdBy   name and version of the program used to create the .torrent
     * @param pieceSize   size of each piece in bytes
     * @return a new {@link BencodedTorrent} instance
     */
    public static BencodedTorrent createNew(Path source, List<List<String>> trackerUrls, String comment,
            String createdBy, int pieceSize) throws IOException {
        if (trackerUrls.isEmpty() || trackerUrls.get(0).isEmpty()) {
            throw new IllegalArgumentException("At least one tracker URL is required");
        }
        Long creationDate = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        String announce = trackerUrls.get(0).get(0);
        BencodedInfo info = BencodedInfoFactory.fromPath(source, pieceSize);
        return new BencodedTorrent(creationDate, announce, trackerUrls, comment, createdBy, info);
    }

    public static BencodedTorrent fromMap(Map<String, Object> map) {
        String announce = getValueAsString(map, KEY_ANNOUNCE).orElseThrow();

        List<List<ByteBuffer>> announceListRaw = getValueAsList(map, KEY_ANNOUNCE_LIST);
        List<List<String>> announceList = announceListRaw.stream()
                .map(buffers -> buffers.stream()
                        .map(buffer -> new String(buffer.array()))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        Long creationDate = getValueAsLong(map, KEY_CREATION_DATE).orElse(null);
        String comment = getValueAsString(map, KEY_COMMENT).orElse("");
        String createdBy = getValueAsString(map, KEY_CREATED_BY).orElse("");

        Map<String, Object> infoDict = getValueAsMap(map, KEY_INFO);
        BencodedInfo info = BencodedInfoFactory.fromMap(infoDict);

        return new BencodedTorrent(creationDate, announce, announceList, comment, createdBy, info);
    }

    public String getAnnounce() {
        return announce;
    }

    public List<List<String>> getAnnounceList() {
        return announceList;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public BencodedInfo getInfo() {
        return info;
    }

    public String getName() {
        return getInfo().getName();
    }

    public long getTotalSize() {
        return getInfo().getTotalSize();
    }

    public Collection<BencodedFile> getFiles() {
        return getInfo().getFiles();
    }

    public Torrent toDomain() {
        try {
            Set<Tracker> trackers = new HashSet<>();
            trackers.add(TrackerFactory.fromUri(URI.create(announce)));
            announceList.stream()
                    .flatMap(List::stream)
                    .map(URI::create)
                    .map(TrackerFactory::fromUri)
                    .collect(Collectors.toCollection(() -> trackers));

            LocalDateTime creationDateTime = LocalDateTime.ofEpochSecond(creationDate, 0,
                    OffsetDateTime.now().getOffset());
            String name = info.getName();
            FileInfo fileInfo = info.toDomain();
            Sha1Hash infoHash = new Sha1Hash(info.getInfoHash());
            return new Torrent(trackers, creationDateTime, comment, createdBy, name, fileInfo, infoHash);
        } catch (Exception e) {
            throw new MappingException("Failed to map BencodedTorrent to Torrent", e);
        }
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
                KEY_ANNOUNCE, announce,
                KEY_ANNOUNCE_LIST, announceList,
                KEY_CREATION_DATE, creationDate,
                KEY_COMMENT, comment,
                KEY_CREATED_BY, createdBy,
                KEY_INFO, info.toMap()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(announce, announceList, creationDate, comment, createdBy, info);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BencodedTorrent that = (BencodedTorrent) o;
        return Objects.equals(announce, that.announce)
                && Objects.equals(announceList, that.announceList)
                && Objects.equals(creationDate, that.creationDate)
                && Objects.equals(comment, that.comment)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(info, that.info);
    }

    @Override
    public String toString() {
        return "TorrentFile{"
                + "announce='" + announce + '\''
                + ", announceList=" + announceList
                + ", creationDate=" + creationDate
                + ", comment='" + comment + '\''
                + ", createdBy='" + createdBy + '\''
                + ", info=" + info
                + '}';
    }
}
