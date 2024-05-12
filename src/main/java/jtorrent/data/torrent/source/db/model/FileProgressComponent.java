package jtorrent.data.torrent.source.db.model;

import java.util.Arrays;
import java.util.BitSet;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.FileProgress;

@Embeddable
public class FileProgressComponent {

    @Column(nullable = false)
    private final byte[] verifiedPieces;

    protected FileProgressComponent() {
        this(new byte[0]);
    }

    public FileProgressComponent(byte[] verifiedPieces) {
        this.verifiedPieces = verifiedPieces;
    }

    public static FileProgressComponent fromDomain(FileProgress fileProgress) {
        byte[] verifiedPieces = fileProgress.getVerifiedPieces().toByteArray();
        return new FileProgressComponent(verifiedPieces);
    }

    public FileProgress toDomain(FileInfo fileInfo, FileMetadata fileMetadata) {
        BitSet domainVerifiedPieces = BitSet.valueOf(verifiedPieces);
        return FileProgress.createExisting(fileInfo, fileMetadata, domainVerifiedPieces);
    }

    public byte[] getVerifiedPieces() {
        return verifiedPieces;
    }

    @Override
    public String toString() {
        return "FileProgressComponent{"
                + ", verifiedPieces=" + Arrays.toString(verifiedPieces)
                + '}';
    }
}
