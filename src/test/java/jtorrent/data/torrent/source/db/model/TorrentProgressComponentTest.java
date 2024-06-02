package jtorrent.data.torrent.source.db.model;

import static jtorrent.data.torrent.source.db.model.testutil.TestUtil.createBitSet;
import static org.instancio.Select.all;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.FileProgress;
import jtorrent.domain.torrent.model.SingleFileInfo;
import jtorrent.domain.torrent.model.TorrentProgress;

class TorrentProgressComponentTest {

    @Test
    void bidirectionalMapping() {
        FileMetadata fileMetadata = Instancio.of(FileMetadata.class)
                .set(field("size"), 49)
                .set(field("path"), Path.of("file.txt"))
                .set(field("firstPiece"), 0)
                .set(field("firstPieceStart"), 0)
                .set(field("lastPiece"), 4)
                .set(field("lastPieceEnd"), 8)
                .set(field("start"), 0)
                .create();

        SingleFileInfo fileInfo = Instancio.of(SingleFileInfo.class)
                .generate(all(byte[].class), gen -> gen.array().length(20))
                .set(field(FileInfo.class, "pieceSize"), 10)
                .generate(field(FileInfo.class, "pieceHashes"), gen -> gen.collection().size(5))
                .set(field(FileInfo.class, "fileMetaData"), List.of(fileMetadata))
                .create();

        TorrentProgress expected = Instancio.of(TorrentProgress.class)
                .set(field("fileInfo"), fileInfo)
                .set(field("pathToFileProgress"),
                        Map.of(
                                Path.of("file.txt"),
                                Instancio.of(FileProgress.class)
                                        .set(field("verifiedPieces"), createBitSet(0, 1, 2, 4))
                                        .set(field("verifiedBytes"), new AtomicLong(39))
                                        .set(field("fileInfo"), fileInfo)
                                        .set(field("fileMetaData"), fileMetadata)
                                        .create()
                        )
                )
                .set(field("verifiedPieces"), createBitSet(0, 1, 2, 4))
                .set(field("completePieces"), createBitSet(0, 1, 2, 4))
                .set(field("pieceIndexToRequestedBlocks"), Map.of())
                .set(field("pieceIndexToAvailableBlocks"),
                        Map.of(
                                0, createBitSet(0),
                                1, createBitSet(0),
                                2, createBitSet(0),
                                4, createBitSet(0)
                        )
                )
                .set(field("partiallyMissingPieces"), new BitSet())
                .set(field("partiallyMissingPiecesWithUnrequestedBlocks"), new BitSet())
                .set(field("completelyMissingPieces"), createBitSet(3))
                .set(field("completelyMissingPiecesWithUnrequestedBlocks"), createBitSet(3))
                .set(field("verifiedBytes"), new AtomicLong(39))
                .set(field("checkedBytes"), 0)
                .create();

        TorrentProgressComponent torrentProgressComponent = TorrentProgressComponent.fromDomain(expected);
        TorrentProgress actual = torrentProgressComponent.toDomain(fileInfo);

        assertEquals(expected, actual);
    }
}
