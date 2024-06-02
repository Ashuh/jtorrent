package jtorrent.data.torrent.source.file.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dampcake.bencode.BencodeOutputStream;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.MultiFileInfo;
import jtorrent.domain.torrent.model.SingleFileInfo;
import jtorrent.domain.torrent.model.TorrentMetadata;

class BencodedTorrentTest {

    @Test
    void decode_singleFile() throws IOException {
        BencodedTorrent expected = new BencodedTorrentBuilder()
                .setAnnounce("announce")
                .setAnnounceList(Collections.emptyList())
                .setCreationDate(123456789L)
                .setComment("comment")
                .setCreatedBy("created by")
                .setInfo(new SingleFileInfoBuilder()
                        .setPieceLength(100)
                        .setLength(100)
                        .setName("name")
                        .setPieces(new byte[20])
                        .build())
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BencodeOutputStream bos = new BencodeOutputStream(baos);
        Map<String, Object> map = Map.of(
                BencodedTorrent.KEY_ANNOUNCE, "announce",
                BencodedTorrent.KEY_ANNOUNCE_LIST, Collections.emptyList(),
                BencodedTorrent.KEY_CREATION_DATE, 123456789L,
                BencodedTorrent.KEY_COMMENT, "comment",
                BencodedTorrent.KEY_CREATED_BY, "created by",
                BencodedTorrent.KEY_INFO, Map.of(
                        BencodedInfo.KEY_PIECE_LENGTH, 100,
                        BencodedInfo.KEY_LENGTH, 100,
                        BencodedInfo.KEY_NAME, "name",
                        BencodedInfo.KEY_PIECES, ByteBuffer.allocate(20)
                )
        );
        bos.writeDictionary(map);
        BencodedTorrent actual = BencodedTorrent.decode(new ByteArrayInputStream(baos.toByteArray()));

        assertEquals(expected, actual);
    }

    @Test
    void decode_multiFile() throws IOException {
        BencodedTorrent expected = new BencodedTorrentBuilder()
                .setAnnounce("announce")
                .setAnnounceList(Collections.emptyList())
                .setCreationDate(123456789L)
                .setComment("comment")
                .setCreatedBy("created by")
                .setInfo(new MultiFileInfoBuilder()
                        .setPieceLength(100)
                        .setName("name")
                        .setPieces(new byte[20])
                        .setFiles(List.of(
                                new BencodedFileBuilder()
                                        .setLength(100)
                                        .setPath(List.of("path1", "path2"))
                                        .build(),
                                new BencodedFileBuilder()
                                        .setLength(200)
                                        .setPath(List.of("path3", "path4"))
                                        .build()
                        ))
                        .build())
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BencodeOutputStream bos = new BencodeOutputStream(baos);
        Map<String, Object> map = Map.of(
                BencodedTorrent.KEY_ANNOUNCE, "announce",
                BencodedTorrent.KEY_ANNOUNCE_LIST, Collections.emptyList(),
                BencodedTorrent.KEY_CREATION_DATE, 123456789L,
                BencodedTorrent.KEY_COMMENT, "comment",
                BencodedTorrent.KEY_CREATED_BY, "created by",
                BencodedTorrent.KEY_INFO, Map.of(
                        BencodedInfo.KEY_PIECE_LENGTH, 100,
                        BencodedInfo.KEY_NAME, "name",
                        BencodedInfo.KEY_PIECES, ByteBuffer.allocate(20),
                        BencodedInfo.KEY_FILES, List.of(
                                Map.of(
                                        "length", 100,
                                        "path", List.of("path1", "path2")
                                ),
                                Map.of(
                                        "length", 200,
                                        "path", List.of("path3", "path4")
                                )
                        )
                )
        );
        bos.writeDictionary(map);
        BencodedTorrent actual = BencodedTorrent.decode(new ByteArrayInputStream(baos.toByteArray()));

        assertEquals(expected, actual);
    }

    @Test
    void bencode_singleFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BencodeOutputStream bos = new BencodeOutputStream(baos);
        Map<String, Object> map = Map.of(
                BencodedTorrent.KEY_ANNOUNCE, "announce",
                BencodedTorrent.KEY_ANNOUNCE_LIST, Collections.emptyList(),
                BencodedTorrent.KEY_CREATION_DATE, 123456789L,
                BencodedTorrent.KEY_COMMENT, "comment",
                BencodedTorrent.KEY_CREATED_BY, "created by",
                BencodedTorrent.KEY_INFO, Map.of(
                        BencodedInfo.KEY_PIECE_LENGTH, 100,
                        BencodedInfo.KEY_LENGTH, 100,
                        BencodedInfo.KEY_NAME, "name",
                        BencodedInfo.KEY_PIECES, ByteBuffer.allocate(20)
                )
        );
        bos.writeDictionary(map);
        byte[] expected = baos.toByteArray();

        BencodedSingleFileInfo bencodedSingleFileInfo = new SingleFileInfoBuilder()
                .setPieceLength(100)
                .setLength(100)
                .setName("name")
                .setPieces(new byte[20])
                .build();
        BencodedTorrent bencodedTorrent = new BencodedTorrentBuilder()
                .setAnnounce("announce")
                .setAnnounceList(Collections.emptyList())
                .setCreationDate(123456789L)
                .setComment("comment")
                .setCreatedBy("created by")
                .setInfo(bencodedSingleFileInfo)
                .build();
        byte[] actual = bencodedTorrent.bencode();

        assertArrayEquals(expected, actual);
    }

    @Test
    void bencode_multiFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BencodeOutputStream bos = new BencodeOutputStream(baos);
        Map<String, Object> map = Map.of(
                BencodedTorrent.KEY_ANNOUNCE, "announce",
                BencodedTorrent.KEY_ANNOUNCE_LIST, Collections.emptyList(),
                BencodedTorrent.KEY_CREATION_DATE, 123456789L,
                BencodedTorrent.KEY_COMMENT, "comment",
                BencodedTorrent.KEY_CREATED_BY, "created by",
                BencodedTorrent.KEY_INFO, Map.of(
                        BencodedInfo.KEY_PIECE_LENGTH, 100,
                        BencodedInfo.KEY_NAME, "name",
                        BencodedInfo.KEY_PIECES, ByteBuffer.allocate(20),
                        BencodedInfo.KEY_FILES, List.of(
                                Map.of(
                                        BencodedFile.KEY_LENGTH, 100,
                                        BencodedFile.KEY_PATH, List.of("path1", "path2")
                                ),
                                Map.of(
                                        BencodedFile.KEY_LENGTH, 200,
                                        BencodedFile.KEY_PATH, List.of("path3", "path4")
                                )
                        )
                )
        );
        bos.writeDictionary(map);
        byte[] expected = baos.toByteArray();

        List<BencodedFile> files = List.of(
                new BencodedFileBuilder()
                        .setLength(100)
                        .setPath(List.of("path1", "path2"))
                        .build(),
                new BencodedFileBuilder()
                        .setLength(200)
                        .setPath(List.of("path3", "path4"))
                        .build()
        );
        BencodedMultiFileInfo bencodedMultiFileInfo = new MultiFileInfoBuilder()
                .setPieceLength(100)
                .setPieces(new byte[20])
                .setName("name")
                .setFiles(files)
                .build();
        BencodedTorrent bencodedTorrent = new BencodedTorrentBuilder()
                .setAnnounce("announce")
                .setAnnounceList(Collections.emptyList())
                .setCreationDate(123456789L)
                .setComment("comment")
                .setCreatedBy("created by")
                .setInfo(bencodedMultiFileInfo)
                .build();
        byte[] actual = bencodedTorrent.bencode();

        assertArrayEquals(expected, actual);
    }

    @Test
    void toDomain_singleFile() throws NoSuchAlgorithmException, IOException {
        BencodedSingleFileInfo info = new SingleFileInfoBuilder()
                .setPieceLength(100)
                .setLength(100)
                .setName("name")
                .setPieces(new byte[20])
                .build();
        BencodedTorrent bencodedTorrent = new BencodedTorrentBuilder()
                .setAnnounce("udp://tracker.example.com:80/announce")
                .setAnnounceList(Collections.emptyList())
                .setCreationDate(123456789L)
                .setComment("comment")
                .setCreatedBy("created by")
                .setInfo(info)
                .build();
        TorrentMetadata actual = bencodedTorrent.toDomain();

        TorrentMetadata expected = new TorrentMetadataBuilder()
                .setTrackers(List.of(List.of(URI.create("udp://tracker.example.com:80/announce"))))
                .setCreationDate(LocalDateTime.ofEpochSecond(123456789L, 0, OffsetDateTime.now().getOffset()))
                .setComment("comment")
                .setCreatedBy("created by")
                .setPieceSize(100)
                .setPieceHashes(List.of(new Sha1Hash(new byte[20])))
                .setName("name")
                .setFileMetadata(List.of(
                        new FileMetadataBuilder()
                                .setLength(100)
                                .setPath(Path.of("name"))
                                .setFirstPiece(0)
                                .setFirstPieceStart(0)
                                .setLastPiece(0)
                                .setLastPieceEnd(99)
                                .setStart(0)
                                .build()
                ))
                .setInfoHash(new Sha1Hash(info.getInfoHash()))
                .build();

        assertEquals(expected, actual);
    }

    @Test
    void toDomain_multiFile() throws NoSuchAlgorithmException, IOException {
        BencodedMultiFileInfo info = new MultiFileInfoBuilder()
                .setPieceLength(100)
                .setPieces(new byte[20])
                .setName("name")
                .setFiles(List.of(
                        new BencodedFileBuilder()
                                .setLength(100)
                                .setPath(List.of("path1", "path2"))
                                .build(),
                        new BencodedFileBuilder()
                                .setLength(200)
                                .setPath(List.of("path3", "path4"))
                                .build()
                ))
                .build();
        BencodedTorrent bencodedTorrent = new BencodedTorrentBuilder()
                .setAnnounce("udp://tracker.example.com:80/announce")
                .setAnnounceList(Collections.emptyList())
                .setCreationDate(123456789L)
                .setComment("comment")
                .setCreatedBy("created by")
                .setInfo(info)
                .build();
        TorrentMetadata actual = bencodedTorrent.toDomain();

        TorrentMetadata expected = new TorrentMetadataBuilder()
                .setTrackers(List.of(List.of(URI.create("udp://tracker.example.com:80/announce"))))
                .setCreationDate(LocalDateTime.ofEpochSecond(123456789L, 0, OffsetDateTime.now().getOffset()))
                .setComment("comment")
                .setCreatedBy("created by")
                .setPieceSize(100)
                .setPieceHashes(List.of(new Sha1Hash(new byte[20])))
                .setName("name")
                .setDirectory("name")
                .setFileMetadata(List.of(
                        new FileMetadataBuilder()
                                .setLength(100)
                                .setPath(Path.of("path1", "path2"))
                                .setFirstPiece(0)
                                .setFirstPieceStart(0)
                                .setLastPiece(0)
                                .setLastPieceEnd(99)
                                .setStart(0)
                                .build(),
                        new FileMetadataBuilder()
                                .setLength(200)
                                .setPath(Path.of("path3", "path4"))
                                .setFirstPiece(1)
                                .setFirstPieceStart(0)
                                .setLastPiece(2)
                                .setLastPieceEnd(99)
                                .setStart(100)
                                .build()
                ))
                .setInfoHash(new Sha1Hash(info.getInfoHash()))
                .build();

        assertEquals(expected, actual);
    }

    private static class BencodedTorrentBuilder {

        private Long creationDate = 0L;
        private String announce = "";
        private List<List<String>> announceList = Collections.emptyList();
        private String comment = "";
        private String createdBy = "";
        private BencodedInfo info = new SingleFileInfoBuilder().build();

        public BencodedTorrentBuilder setCreationDate(Long creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public BencodedTorrentBuilder setAnnounce(String announce) {
            this.announce = announce;
            return this;
        }

        public BencodedTorrentBuilder setAnnounceList(List<List<String>> announceList) {
            this.announceList = announceList;
            return this;
        }

        public BencodedTorrentBuilder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public BencodedTorrentBuilder setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public BencodedTorrentBuilder setInfo(BencodedInfo info) {
            this.info = info;
            return this;
        }

        public BencodedTorrent build() {
            return new BencodedTorrent(creationDate, announce, announceList, comment, createdBy, info);
        }
    }

    private static class SingleFileInfoBuilder {

        private int length = 0;
        private String name = "";
        private int pieceLength = 0;
        private byte[] pieces = new byte[0];

        public SingleFileInfoBuilder setLength(int length) {
            this.length = length;
            return this;
        }

        public SingleFileInfoBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SingleFileInfoBuilder setPieceLength(int pieceLength) {
            this.pieceLength = pieceLength;
            return this;
        }

        public SingleFileInfoBuilder setPieces(byte[] pieces) {
            this.pieces = pieces;
            return this;
        }

        public BencodedSingleFileInfo build() {
            return new BencodedSingleFileInfo(pieceLength, pieces, name, length);
        }
    }

    private static class MultiFileInfoBuilder {

        private int pieceLength = 0;
        private byte[] pieces = new byte[0];
        private String name = "";
        private List<BencodedFile> files = Collections.emptyList();

        public MultiFileInfoBuilder setPieceLength(int pieceLength) {
            this.pieceLength = pieceLength;
            return this;
        }

        public MultiFileInfoBuilder setPieces(byte[] pieces) {
            this.pieces = pieces;
            return this;
        }

        public MultiFileInfoBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public MultiFileInfoBuilder setFiles(List<BencodedFile> files) {
            this.files = files;
            return this;
        }

        public BencodedMultiFileInfo build() {
            return new BencodedMultiFileInfo(pieceLength, pieces, name, files);
        }
    }

    private static class BencodedFileBuilder {

        private int length = 0;
        private List<String> path = Collections.emptyList();

        public BencodedFileBuilder setLength(int length) {
            this.length = length;
            return this;
        }

        public BencodedFileBuilder setPath(List<String> path) {
            this.path = path;
            return this;
        }

        public BencodedFile build() {
            return new BencodedFile(length, path);
        }
    }

    private static class TorrentMetadataBuilder {

        private List<List<URI>> trackers = Collections.emptyList();
        private LocalDateTime creationDate = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        private String comment = "";
        private String createdBy = "";
        private int pieceSize = 0;
        private List<Sha1Hash> pieceHashes = Collections.emptyList();
        private String name = "";
        private String directory;
        private List<FileMetadata> fileMetadata = Collections.emptyList();
        private Sha1Hash infoHash = new Sha1Hash(new byte[20]);

        public TorrentMetadataBuilder setTrackers(List<List<URI>> trackers) {
            this.trackers = trackers;
            return this;
        }

        public TorrentMetadataBuilder setCreationDate(LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public TorrentMetadataBuilder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public TorrentMetadataBuilder setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public TorrentMetadataBuilder setPieceSize(int pieceSize) {
            this.pieceSize = pieceSize;
            return this;
        }

        public TorrentMetadataBuilder setPieceHashes(List<Sha1Hash> pieceHashes) {
            this.pieceHashes = pieceHashes;
            return this;
        }

        public TorrentMetadataBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public TorrentMetadataBuilder setDirectory(String directory) {
            this.directory = directory;
            return this;
        }

        public TorrentMetadataBuilder setFileMetadata(List<FileMetadata> fileMetadata) {
            this.fileMetadata = fileMetadata;
            return this;
        }

        public TorrentMetadataBuilder setInfoHash(Sha1Hash infoHash) {
            this.infoHash = infoHash;
            return this;
        }

        public TorrentMetadata build() {
            FileInfo fileInfo;
            if (directory == null) {
                if (fileMetadata.size() != 1) {
                    throw new IllegalArgumentException("Number of files must be 1 if directory is null");
                }
                FileMetadata fileMetadata = this.fileMetadata.get(0);
                fileInfo = new SingleFileInfo(fileMetadata, pieceSize, pieceHashes, infoHash);
            } else {
                fileInfo = new MultiFileInfo(directory, fileMetadata, pieceSize, pieceHashes, infoHash);
            }
            return new TorrentMetadata(trackers, creationDate, comment, createdBy, fileInfo);
        }
    }

    private static class FileMetadataBuilder {

        private int length;
        private Path path = Path.of("");
        private int firstPiece;
        private int firstPieceStart;
        private int lastPiece;
        private int lastPieceEnd;
        private long start;

        public FileMetadataBuilder setLength(int length) {
            this.length = length;
            return this;
        }

        public FileMetadataBuilder setPath(Path path) {
            this.path = path;
            return this;
        }

        public FileMetadataBuilder setFirstPiece(int firstPiece) {
            this.firstPiece = firstPiece;
            return this;
        }

        public FileMetadataBuilder setFirstPieceStart(int firstPieceStart) {
            this.firstPieceStart = firstPieceStart;
            return this;
        }

        public FileMetadataBuilder setLastPiece(int lastPiece) {
            this.lastPiece = lastPiece;
            return this;
        }

        public FileMetadataBuilder setLastPieceEnd(int lastPieceEnd) {
            this.lastPieceEnd = lastPieceEnd;
            return this;
        }

        public FileMetadataBuilder setStart(long start) {
            this.start = start;
            return this;
        }

        public FileMetadata build() {
            return new FileMetadata(path, start, length, firstPiece, firstPieceStart, lastPiece, lastPieceEnd);
        }
    }
}
