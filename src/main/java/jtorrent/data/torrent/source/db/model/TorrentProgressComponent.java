package jtorrent.data.torrent.source.db.model;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.FileProgress;
import jtorrent.domain.torrent.model.TorrentProgress;

@Embeddable
public class TorrentProgressComponent {

    @ElementCollection
    @CollectionTable
    private final Map<Integer, BitSet> pieceToReceivedBlocks;

    @Column(nullable = false)
    private final byte[] verifiedPieces;

    protected TorrentProgressComponent() {
        this(Collections.emptyMap(), new byte[0]);
    }

    public TorrentProgressComponent(Map<Integer, BitSet> pieceToReceivedBlocks, byte[] verifiedPieces) {
        this.pieceToReceivedBlocks = pieceToReceivedBlocks;
        this.verifiedPieces = verifiedPieces;
    }

    public static TorrentProgressComponent fromDomain(TorrentProgress torrentProgress) {
        byte[] verifiedPieces = torrentProgress.getVerifiedPieces().toByteArray();
        Map<Integer, BitSet> pieceToReceivedBlocks = torrentProgress.getReceivedBlocks();
        return new TorrentProgressComponent(pieceToReceivedBlocks, verifiedPieces);
    }

    public TorrentProgress toDomain(FileInfo fileInfo) {
        BitSet domainVerifiedPieces = BitSet.valueOf(verifiedPieces);
        Map<Path, FileProgress> domainFileProgress = fileInfo.getFileMetaData().stream()
                .map(FileMetadata::path)
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                path -> FileProgress.createExisting(fileInfo, fileInfo.getFileMetaData(path),
                                        domainVerifiedPieces)
                        )
                );
        return TorrentProgress.createExisting(fileInfo, domainFileProgress, domainVerifiedPieces,
                pieceToReceivedBlocks);
    }

    public Map<Integer, BitSet> getPieceToReceivedBlocks() {
        return pieceToReceivedBlocks;
    }

    public byte[] getVerifiedPieces() {
        return verifiedPieces;
    }

    @Override
    public int hashCode() {
        int result = pieceToReceivedBlocks.hashCode();
        result = 31 * result + Arrays.hashCode(verifiedPieces);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TorrentProgressComponent that = (TorrentProgressComponent) o;
        return pieceToReceivedBlocks.equals(that.pieceToReceivedBlocks)
                && Arrays.equals(verifiedPieces, that.verifiedPieces);
    }

    @Override
    public String toString() {
        return "TorrentProgressComponent{"
                + ", pieceToReceivedBlocks=" + pieceToReceivedBlocks
                + ", verifiedPieces=" + Arrays.toString(verifiedPieces)
                + '}';
    }
}
