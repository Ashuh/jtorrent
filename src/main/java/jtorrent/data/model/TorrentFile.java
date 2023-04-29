package jtorrent.data.model;

import static jtorrent.data.util.MapUtil.getValueAsList;
import static jtorrent.data.util.MapUtil.getValueAsLong;
import static jtorrent.data.util.MapUtil.getValueAsMap;
import static jtorrent.data.util.MapUtil.getValueAsString;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.dampcake.bencode.BencodeInputStream;

import jtorrent.data.model.info.Info;

public class TorrentFile extends BencodedObject {

    private static final String KEY_ANNOUNCE = "announce";
    private static final String KEY_ANNOUNCE_LIST = "announce-list";
    private static final String KEY_CREATION_DATE = "creation date";
    private static final String KEY_COMMENT = "comment";
    private static final String KEY_CREATED_BY = "created by";
    private static final String KEY_INFO = "info";

    private final String announce;
    private final List<List<String>> announceList;
    private final Long creationDate;
    private final String comment;
    private final String createdBy;
    private final Info info;

    public TorrentFile(Long creationDate, String announce, List<List<String>> announceList,
            String comment, String createdBy, Info info) {
        this.announce = announce;
        this.announceList = announceList;
        this.creationDate = creationDate;
        this.comment = comment;
        this.createdBy = createdBy;
        this.info = info;
    }

    public static TorrentFile decode(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        BencodeInputStream bis = new BencodeInputStream(inputStream, StandardCharsets.UTF_8, true);
        Map<String, Object> topLevelDict = bis.readDictionary();
        return fromMap(topLevelDict);
    }

    public static TorrentFile fromMap(Map<String, Object> map) {
        String announce = getValueAsString(map, KEY_ANNOUNCE).orElse(null);

        List<List<ByteBuffer>> annouceListRaw = getValueAsList(map, KEY_ANNOUNCE_LIST);
        List<List<String>> announceList = annouceListRaw.stream()
                .map(buffers -> buffers.stream()
                        .map(buffer -> new String(buffer.array()))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        Long creationDate = getValueAsLong(map, KEY_CREATION_DATE).orElse(null);
        String comment = getValueAsString(map, KEY_COMMENT).orElse("");
        String createdBy = getValueAsString(map, KEY_CREATED_BY).orElse("");

        Map<String, Object> infoDict = getValueAsMap(map, KEY_INFO);
        Info info = Info.fromMap(infoDict);

        return new TorrentFile(creationDate, announce, announceList, comment, createdBy, info);
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

    public Info getInfo() {
        return info;
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
        TorrentFile that = (TorrentFile) o;
        return Objects.equals(announce, that.announce)
                && Objects.equals(announceList, that.announceList)
                && Objects.equals(creationDate, that.creationDate)
                && Objects.equals(comment, that.comment)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(info, that.info);
    }

    @Override
    public String toString() {
        return "TorrentFile{" +
                "announce='" + announce + '\'' +
                ", announceList=" + announceList +
                ", creationDate=" + creationDate +
                ", comment='" + comment + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", info=" + info +
                '}';
    }
}
