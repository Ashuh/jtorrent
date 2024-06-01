package jtorrent.data.torrent.source.db.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Formula;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.MultiFileInfo;
import jtorrent.domain.torrent.model.SingleFileInfo;

@Embeddable
public class FileInfoComponent {

    @Column
    private final String directory;

    @OrderColumn
    @ElementCollection
    private final List<FileMetadataComponent> fileMetadata;

    @Lob
    private final byte[] pieceHashes;

    @Column(nullable = false)
    private final int pieceSize;

    @Formula("infoHash")
    private final byte[] infoHash;

    protected FileInfoComponent() {
        this(null, Collections.emptyList(), new byte[0], 0, null);
    }

    public FileInfoComponent(String directory, List<FileMetadataComponent> fileMetadata, byte[] pieceHashes,
            int pieceSize, byte[] infoHash) {
        this.directory = directory;
        this.fileMetadata = fileMetadata;
        this.pieceHashes = pieceHashes;
        this.pieceSize = pieceSize;
        this.infoHash = infoHash;
    }

    public static FileInfoComponent fromDomain(FileInfo fileInfo) {
        if (fileInfo instanceof SingleFileInfo singleFileInfo) {
            return fromDomain(singleFileInfo);
        } else {
            return fromDomain((MultiFileInfo) fileInfo);
        }
    }

    public static FileInfoComponent fromDomain(SingleFileInfo singleFileInfo) {
        List<FileMetadataComponent> fileMetadata = singleFileInfo.getFileMetaData().stream()
                .map(FileMetadataComponent::fromDomain)
                .toList();
        byte[] pieceHashes = Sha1Hash.concatHashes(singleFileInfo.getPieceHashes());
        Sha1Hash.concatHashes(singleFileInfo.getPieceHashes());
        int pieceSize = singleFileInfo.getPieceSize();
        byte[] infoHash = singleFileInfo.getInfoHash().getBytes();
        return new FileInfoComponent(null, fileMetadata, pieceHashes, pieceSize, infoHash);
    }

    public static FileInfoComponent fromDomain(MultiFileInfo multiFileInfo) {
        String directory = multiFileInfo.getDirectory();
        List<FileMetadataComponent> fileMetadata = multiFileInfo.getFileMetaData().stream()
                .map(FileMetadataComponent::fromDomain)
                .toList();
        byte[] pieceHashes = Sha1Hash.concatHashes(multiFileInfo.getPieceHashes());
        int pieceSize = multiFileInfo.getPieceSize();
        byte[] infoHash = multiFileInfo.getInfoHash().getBytes();
        return new FileInfoComponent(directory,  List.of(), pieceHashes, pieceSize, infoHash);
    }

    public FileInfo toDomain() {
        return isSingleFile() ? toSingleFileInfo() : toMultiFileInfo();
    }

    private boolean isSingleFile() {
        return directory == null;
    }

    private SingleFileInfo toSingleFileInfo() {
        FileMetadata domainFileMetadata = fileMetadata.get(0).toDomain();
        List<Sha1Hash> domainPieceHashes = Sha1Hash.splitHashes(pieceHashes);
        Sha1Hash domainInfoHash = new Sha1Hash(infoHash);
        return new SingleFileInfo(domainFileMetadata, pieceSize, domainPieceHashes, domainInfoHash);
    }

    private MultiFileInfo toMultiFileInfo() {
        List<FileMetadata> domainFileMetadata = fileMetadata.stream()
                .map(FileMetadataComponent::toDomain)
                .toList();
        List<Sha1Hash> domainPieceHashes = Sha1Hash.splitHashes(pieceHashes);
        Sha1Hash domainInfoHash = new Sha1Hash(infoHash);
        return new MultiFileInfo(directory, domainFileMetadata, pieceSize, domainPieceHashes, domainInfoHash);
    }

    public String getDirectory() {
        return directory;
    }

    public List<FileMetadataComponent> getFileMetadata() {
        return fileMetadata;
    }

    public byte[] getPieceHashes() {
        return pieceHashes;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(directory);
        result = 31 * result + fileMetadata.hashCode();
        result = 31 * result + Arrays.hashCode(pieceHashes);
        result = 31 * result + pieceSize;
        result = 31 * result + Arrays.hashCode(infoHash);
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

        FileInfoComponent that = (FileInfoComponent) o;
        return pieceSize == that.pieceSize
                && Objects.equals(directory, that.directory)
                && fileMetadata.equals(that.fileMetadata)
                && Arrays.equals(pieceHashes, that.pieceHashes)
                && Arrays.equals(infoHash, that.infoHash);
    }

    @Override
    public String toString() {
        return "FileInfoComponent{"
                + "directory='" + directory + '\''
                + ", fileMetadata=" + fileMetadata
                + ", pieceHashes=" + Arrays.toString(pieceHashes)
                + ", pieceSize=" + pieceSize
                + ", infoHash=" + Arrays.toString(infoHash)
                + '}';
    }
}
