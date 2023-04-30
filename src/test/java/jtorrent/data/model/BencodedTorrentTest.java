package jtorrent.data.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dampcake.bencode.BencodeOutputStream;

import jtorrent.data.model.info.BencodedFile;
import jtorrent.data.model.info.BencodedInfo;
import jtorrent.data.model.info.MultiFileInfo;
import jtorrent.data.model.info.SingleFileInfo;

class BencodedTorrentTest {

    @Test
    void decode_singleFile() throws IOException, NoSuchAlgorithmException {
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
    void decode_multiFile() throws IOException, NoSuchAlgorithmException {
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

        SingleFileInfo singleFileInfo = new SingleFileInfoBuilder()
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
                .setInfo(singleFileInfo)
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
        MultiFileInfo multiFileInfo = new MultiFileInfoBuilder()
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
                .setInfo(multiFileInfo)
                .build();
        byte[] actual = bencodedTorrent.bencode();

        assertArrayEquals(expected, actual);
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

        public SingleFileInfo build() {
            return new SingleFileInfo(pieceLength, pieces, name, length);
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

        public MultiFileInfo build() {
            return new MultiFileInfo(pieceLength, pieces, name, files);
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
}
