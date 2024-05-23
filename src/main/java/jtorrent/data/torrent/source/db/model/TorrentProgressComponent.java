package jtorrent.data.torrent.source.db.model;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileProgress;
import jtorrent.domain.torrent.model.TorrentProgress;

@Embeddable
public class TorrentProgressComponent {

    @ElementCollection
    @CollectionTable
    private final Map<String, FileProgressComponent> pathToFileProgress;

    @ElementCollection
    @CollectionTable
    private final Map<Integer, BitSet> pieceToReceivedBlocks;

    @Column(nullable = false)
    private final byte[] verifiedPieces;

    protected TorrentProgressComponent() {
        this(Collections.emptyMap(), Collections.emptyMap(), new byte[0]);
    }

    public TorrentProgressComponent(Map<String, FileProgressComponent> pathToFileProgress,
            Map<Integer, BitSet> pieceToReceivedBlocks, byte[] verifiedPieces) {
        this.pathToFileProgress = pathToFileProgress;
        this.pieceToReceivedBlocks = pieceToReceivedBlocks;
        this.verifiedPieces = verifiedPieces;
    }

    public static TorrentProgressComponent fromDomain(TorrentProgress torrentProgress) {
        byte[] verifiedPieces = torrentProgress.getVerifiedPieces().toByteArray();
        Map<String, FileProgressComponent> pathToFileProgress = torrentProgress.getFileProgress().entrySet().stream()
                .map(e -> Map.entry(e.getKey().toString(), FileProgressComponent.fromDomain(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<Integer, BitSet> pieceToReceivedBlocks = torrentProgress.getReceivedBlocks();
        return new TorrentProgressComponent(pathToFileProgress, pieceToReceivedBlocks, verifiedPieces);
    }

    public TorrentProgress toDomain(FileInfo fileInfo) {
        Map<Path, FileProgress> domainFileProgress = pathToFileProgress.entrySet().stream()
                .map(e -> {
                    Path path = Path.of(e.getKey());
                    FileProgress fileProgress = e.getValue().toDomain(fileInfo, fileInfo.getFileMetaData(path));
                    return Map.entry(path, fileProgress);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        BitSet domainVerifiedPieces = BitSet.valueOf(verifiedPieces);
        return TorrentProgress.createExisting(fileInfo, domainFileProgress, domainVerifiedPieces,
                pieceToReceivedBlocks);
    }

    public Map<String, FileProgressComponent> getPathToFileProgress() {
        return pathToFileProgress;
    }

    public Map<Integer, BitSet> getPieceToReceivedBlocks() {
        return pieceToReceivedBlocks;
    }

    public byte[] getVerifiedPieces() {
        return verifiedPieces;
    }

    @Override
    public int hashCode() {
        int result = pathToFileProgress.hashCode();
        result = 31 * result + pieceToReceivedBlocks.hashCode();
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
        return pathToFileProgress.equals(that.pathToFileProgress)
                && pieceToReceivedBlocks.equals(that.pieceToReceivedBlocks)
                && Arrays.equals(verifiedPieces, that.verifiedPieces);
    }

    @Override
    public String toString() {
        return "TorrentProgressComponent{"
                + "pathToFileProgress=" + pathToFileProgress
                + ", pieceToReceivedBlocks=" + pieceToReceivedBlocks
                + ", verifiedPieces=" + Arrays.toString(verifiedPieces)
                + '}';
    }
}
