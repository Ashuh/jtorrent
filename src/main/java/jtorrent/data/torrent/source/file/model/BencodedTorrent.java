package jtorrent.data.torrent.source.file.model;

import static jtorrent.data.torrent.source.file.model.util.MapUtil.getValueAsList;
import static jtorrent.data.torrent.source.file.model.util.MapUtil.getValueAsLong;
import static jtorrent.data.torrent.source.file.model.util.MapUtil.getValueAsMap;
import static jtorrent.data.torrent.source.file.model.util.MapUtil.getValueAsString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.dampcake.bencode.BencodeInputStream;

import jtorrent.data.torrent.source.file.model.exception.MappingException;
import jtorrent.domain.common.util.bencode.BencodedObject;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.TorrentMetadata;

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

    public static BencodedTorrent withAnnounceList(Long creationDate, List<List<String>> announceList, String comment,
            String createdBy, BencodedInfo info) {
        if (announceList.isEmpty() || announceList.get(0).isEmpty()) {
            throw new IllegalArgumentException("At least one tracker is required");
        }
        String announce = announceList.get(0).get(0);
        return new BencodedTorrent(creationDate, announce, announceList, comment, createdBy, info);
    }

    public static BencodedTorrent withAnnounce(Long creationDate, String announce, String comment, String createdBy,
            BencodedInfo info) {
        return new BencodedTorrent(creationDate, announce, null, comment, createdBy, info);
    }

    public static BencodedTorrent decode(InputStream inputStream) throws IOException {
        BencodeInputStream bis = new BencodeInputStream(inputStream, StandardCharsets.UTF_8, true);
        Map<String, Object> topLevelDict = bis.readDictionary();
        return fromMap(topLevelDict);
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

    public static BencodedTorrent fromDomain(TorrentMetadata torrentMetadata) {
        Long creationDate = torrentMetadata.creationDate().toEpochSecond(OffsetDateTime.now().getOffset());
        // TODO: proper handling of tracker groups
        String announce = torrentMetadata.trackerTiers().get(0).get(0).toString();
        List<List<String>> announceList = torrentMetadata.trackerTiers().stream()
                .map(tier -> tier.stream()
                        .map(URI::toString)
                        .toList()
                )
                .toList();
        BencodedInfo info = BencodedInfoFactory.fromDomain(torrentMetadata.fileInfo());
        return new BencodedTorrent(creationDate, announce, announceList, torrentMetadata.comment(),
                torrentMetadata.createdBy(), info);
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

    public TorrentMetadata toDomain() {
        try {
            final List<List<URI>> trackers;

            if (announceList != null && !announceList.isEmpty()) {
                trackers = announceList.stream()
                        .map(tier -> tier.stream()
                                        .map(URI::create)
                                        .toList()
                        ).toList();
            } else {
                trackers = List.of(List.of(URI.create(announce)));
            }

            LocalDateTime creationDateTime = LocalDateTime.ofEpochSecond(creationDate, 0,
                    OffsetDateTime.now().getOffset());
            FileInfo fileInfo = info.toDomain();
            return new TorrentMetadata(trackers, creationDateTime, comment, createdBy, fileInfo);
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
