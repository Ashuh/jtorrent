package jtorrent.data.model;

import static jtorrent.data.util.MapUtil.getValueAsList;
import static jtorrent.data.util.MapUtil.getValueAsLong;
import static jtorrent.data.util.MapUtil.getValueAsMap;
import static jtorrent.data.util.MapUtil.getValueAsString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.dampcake.bencode.BencodeInputStream;

import jtorrent.data.model.exception.MappingException;
import jtorrent.data.model.info.BencodedFile;
import jtorrent.data.model.info.BencodedInfo;
import jtorrent.data.model.info.BencodedInfoFactory;
import jtorrent.domain.model.torrent.File;
import jtorrent.domain.model.torrent.Sha1Hash;
import jtorrent.domain.model.torrent.Torrent;

public class BencodedTorrent extends BencodedObject {

    public static final String KEY_ANNOUNCE = "announce";
    public static final String KEY_ANNOUNCE_LIST = "announce-list";
    public static final String KEY_CREATION_DATE = "creation date";
    public static final String KEY_COMMENT = "comment";
    public static final String KEY_CREATED_BY = "created by";
    public static final String KEY_INFO = "info";

    private final String announce;
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

    public static BencodedTorrent decode(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        BencodeInputStream bis = new BencodeInputStream(inputStream, StandardCharsets.UTF_8, true);
        Map<String, Object> topLevelDict = bis.readDictionary();
        return fromMap(topLevelDict);
    }

    public static BencodedTorrent fromMap(Map<String, Object> map) {
        String announce = getValueAsString(map, KEY_ANNOUNCE).orElseThrow();

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

    public Torrent toDomain() {
        try {
            // ignore announceList for now
            List<URI> trackers = List.of(new URI(announce));
            LocalDateTime creationDateTime = LocalDateTime.ofEpochSecond(creationDate, 0, ZoneOffset.UTC);
            List<Sha1Hash> pieceHashes = mapPieces(info.getPieces());
            int pieceLength = info.getPieceLength();
            String name = info.getName();
            List<File> files = mapFiles(info.getFiles());
            Sha1Hash infoHash = new Sha1Hash(info.getInfoHash());
            return new Torrent(trackers, creationDateTime, comment, createdBy,
                    pieceLength, pieceHashes, name, files, infoHash);
        } catch (Exception e) {
            throw new MappingException("Failed to map BencodedTorrent to Torrent", e);
        }
    }

    private List<Sha1Hash> mapPieces(byte[] pieces) {
        List<Sha1Hash> pieceHashes = new ArrayList<>();
        for (int i = 0; i < pieces.length; i += Sha1Hash.HASH_SIZE) {
            byte[] pieceHash = new byte[Sha1Hash.HASH_SIZE];
            System.arraycopy(pieces, i, pieceHash, 0, Sha1Hash.HASH_SIZE);
            pieceHashes.add(new Sha1Hash(pieceHash));
        }
        return pieceHashes;
    }

    private List<File> mapFiles(List<BencodedFile> files) {
        return files.stream()
                .map(BencodedFile::toDomain)
                .collect(Collectors.toList());
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
