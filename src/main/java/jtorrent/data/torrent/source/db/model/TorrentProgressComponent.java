package jtorrent.data.torrent.source.db.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.FileProgress;
import jtorrent.domain.torrent.model.TorrentProgress;

@Embeddable
public class TorrentProgressComponent {

    @Lob
    @Column(nullable = false)
    private final byte[] verifiedPieces;

    @Lob
    @Column(nullable = false)
    private byte[] pieceToReceivedBlocks;

    protected TorrentProgressComponent() {
        this(new byte[0], new byte[0]);
    }

    public TorrentProgressComponent(byte[] pieceToReceivedBlocks, byte[] verifiedPieces) {
        this.pieceToReceivedBlocks = pieceToReceivedBlocks;
        this.verifiedPieces = verifiedPieces;
    }

    public static TorrentProgressComponent fromDomain(TorrentProgress torrentProgress) {
        byte[] pieceToReceivedBlocks = serializeMap(torrentProgress.getReceivedBlocks());
        byte[] verifiedPieces = torrentProgress.getVerifiedPieces().toByteArray();
        return new TorrentProgressComponent(pieceToReceivedBlocks, verifiedPieces);
    }

    private static byte[] serializeMap(Map<Integer, BitSet> map) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(map.size());
            for (Map.Entry<Integer, BitSet> entry : map.entrySet()) {
                dos.writeInt(entry.getKey());
                byte[] bitsetBytes = entry.getValue().toByteArray();
                dos.writeInt(bitsetBytes.length);
                dos.write(bitsetBytes);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
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
        Map<Integer, BitSet> domainPieceToReceivedBlocks = deserializeMap(pieceToReceivedBlocks);
        return TorrentProgress.createExisting(fileInfo, domainFileProgress, domainVerifiedPieces,
                domainPieceToReceivedBlocks);
    }

    private static Map<Integer, BitSet> deserializeMap(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {
            int size = dis.readInt();
            Map<Integer, BitSet> map = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                int key = dis.readInt();
                int length = dis.readInt();
                byte[] bitsetBytes = new byte[length];
                dis.readFully(bitsetBytes);
                BitSet bitSet = BitSet.valueOf(bitsetBytes);
                map.put(key, bitSet);
            }
            return map;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public byte[] getVerifiedPieces() {
        return verifiedPieces;
    }

    public byte[] getPieceToReceivedBlocks() {
        return pieceToReceivedBlocks;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(verifiedPieces);
        result = 31 * result + Arrays.hashCode(pieceToReceivedBlocks);
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
        return Arrays.equals(verifiedPieces, that.verifiedPieces)
                && Arrays.equals(pieceToReceivedBlocks, that.pieceToReceivedBlocks);
    }

    @Override
    public String toString() {
        return "TorrentProgressComponent{"
                + ", pieceToReceivedBlocks=" + Arrays.toString(pieceToReceivedBlocks)
                + ", verifiedPieces=" + Arrays.toString(verifiedPieces)
                + '}';
    }
}
