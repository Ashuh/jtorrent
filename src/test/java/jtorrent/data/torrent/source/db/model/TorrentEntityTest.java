package jtorrent.data.torrent.source.db.model;

import static jtorrent.data.torrent.source.db.model.testutil.TestUtil.createBitSetWithRange;
import static org.instancio.Assign.valueOf;
import static org.instancio.Select.all;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.instancio.Instancio;
import org.instancio.Model;
import org.junit.jupiter.api.Test;

import jtorrent.domain.common.util.rx.MutableRxObservableSet;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.FileProgress;
import jtorrent.domain.torrent.model.MultiFileInfo;
import jtorrent.domain.torrent.model.SingleFileInfo;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.domain.torrent.model.TorrentProgress;
import jtorrent.domain.tracker.model.factory.TrackerFactory;

class TorrentEntityTest {

    private static final Model<TorrentProgress> TORRENT_PROGRESS_MODEL = Instancio.of(TorrentProgress.class)
            .set(field("verifiedPieces"), new BitSet())
            .set(field("completePieces"), new BitSet())
            .set(field("pieceIndexToRequestedBlocks"), Map.of())
            .set(field("pieceIndexToAvailableBlocks"), Map.of())
            .set(field("partiallyMissingPieces"), new BitSet())
            .set(field("partiallyMissingPiecesWithUnrequestedBlocks"), new BitSet())
            .set(field("completelyMissingPieces"), new BitSet())
            .set(field("completelyMissingPiecesWithUnrequestedBlocks"), new BitSet())
            .set(field("verifiedBytes"), new AtomicLong(0))
            .set(field("checkedBytes"), 0)
            .toModel();

    private static final Model<FileProgress> FILE_PROGRESS_MODEL = Instancio.of(FileProgress.class)
            .set(field("verifiedPieces"), new BitSet())
            .set(field("verifiedBytes"), new AtomicLong(0))
            .toModel();

    @Test
    void bidirectionalMapping_singleFile() {
        FileMetadata fileMetadata = Instancio.of(FileMetadata.class)
                .set(field("size"), 100)
                .set(field("path"), Path.of("file.txt"))
                .set(field("firstPiece"), 0)
                .set(field("firstPieceStart"), 0)
                .set(field("lastPiece"), 9)
                .set(field("lastPieceEnd"), 9)
                .set(field("start"), 0)
                .create();

        SingleFileInfo fileInfo = Instancio.of(SingleFileInfo.class)
                .generate(all(byte[].class), gen -> gen.array().length(20))
                .generate(field(FileInfo.class, "pieceHashes"), gen -> gen.collection().size(10))
                .set(field(FileInfo.class, "pieceSize"), 10)
                .set(field(FileInfo.class, "fileMetaData"), List.of(fileMetadata))
                .create();

        Torrent expected = Instancio.of(Torrent.class)
                .assign(valueOf(field(TorrentMetadata.class, "trackerTiers"))
                        .to(field(Torrent.class, "trackers"))
                        .as(trackerTiers -> ((List<List<URI>>) trackerTiers)
                                .get(0)
                                .stream()
                                .map(TrackerFactory::fromUri)
                                .collect(Collectors.toSet())
                        )
                )
                .set(field("peers"), new MutableRxObservableSet<>(Set.of()))
                .set(field(TorrentMetadata.class, "fileInfo"), fileInfo)
                .set(field("torrentProgress"),
                        Instancio.of(TORRENT_PROGRESS_MODEL)
                                .set(field("fileInfo"), fileInfo)
                                .set(field("completelyMissingPieces"), createBitSetWithRange(0, 10))
                                .set(field("completelyMissingPiecesWithUnrequestedBlocks"),
                                        createBitSetWithRange(0, 10))
                                .set(field("pathToFileProgress"),
                                        Map.of(
                                                Path.of("file.txt"),
                                                Instancio.of(FILE_PROGRESS_MODEL)
                                                        .set(field("fileInfo"), fileInfo)
                                                        .set(field("fileMetaData"), fileMetadata)
                                                        .create()
                                        )
                                )
                                .create()
                )
                .create();

        TorrentEntity torrentEntity = TorrentEntity.fromDomain(expected);
        Torrent actual = torrentEntity.toDomain();

        assertEquals(expected, actual);
    }

    @Test
    void bidirectionalMapping_multiFile() {
        FileMetadata fileMetadata1 = Instancio.of(FileMetadata.class)
                .set(field("size"), 100)
                .set(field("path"), Path.of("file1.txt"))
                .set(field("firstPiece"), 0)
                .set(field("firstPieceStart"), 0)
                .set(field("lastPiece"), 9)
                .set(field("lastPieceEnd"), 9)
                .set(field("start"), 0)
                .create();

        FileMetadata fileMetadata2 = Instancio.of(FileMetadata.class)
                .set(field("size"), 100)
                .set(field("path"), Path.of("file2.txt"))
                .set(field("firstPiece"), 10)
                .set(field("firstPieceStart"), 0)
                .set(field("lastPiece"), 19)
                .set(field("lastPieceEnd"), 9)
                .set(field("start"), 100)
                .create();

        MultiFileInfo fileInfo = Instancio.of(MultiFileInfo.class)
                .generate(all(byte[].class), gen -> gen.array().length(20))
                .generate(field(FileInfo.class, "pieceHashes"), gen -> gen.collection().size(20))
                .set(field(FileInfo.class, "pieceSize"), 10)
                .set(field(FileInfo.class, "fileMetaData"),
                        List.of(
                                fileMetadata1,
                                fileMetadata2
                        )
                )
                .create();

        Torrent expected = Instancio.of(Torrent.class)
                .assign(valueOf(field(TorrentMetadata.class, "trackerTiers"))
                        .to(field(Torrent.class, "trackers"))
                        .as(trackerTiers -> ((List<List<URI>>) trackerTiers)
                                .get(0)
                                .stream()
                                .map(TrackerFactory::fromUri)
                                .collect(Collectors.toSet())
                        )
                )
                .set(field("peers"), new MutableRxObservableSet<>(Set.of()))
                .set(field(TorrentMetadata.class, "fileInfo"), fileInfo)
                .set(field("torrentProgress"),
                        Instancio.of(TORRENT_PROGRESS_MODEL)
                                .set(field("fileInfo"), fileInfo)
                                .set(field("completelyMissingPieces"), createBitSetWithRange(0, 20))
                                .set(field("completelyMissingPiecesWithUnrequestedBlocks"),
                                        createBitSetWithRange(0, 20))
                                .set(field("pathToFileProgress"),
                                        Map.of(
                                                Path.of("file1.txt"),
                                                Instancio.of(FILE_PROGRESS_MODEL)
                                                        .set(field("fileInfo"), fileInfo)
                                                        .set(field("fileMetaData"), fileMetadata1)
                                                        .create(),
                                                Path.of("file2.txt"),
                                                Instancio.of(FILE_PROGRESS_MODEL)
                                                        .set(field("fileInfo"), fileInfo)
                                                        .set(field("fileMetaData"), fileMetadata2)
                                                        .create()
                                        )
                                )
                                .create()
                )
                .create();

        TorrentEntity torrentEntity = TorrentEntity.fromDomain(expected);
        Torrent actual = torrentEntity.toDomain();

        assertEquals(expected, actual);
    }
}
