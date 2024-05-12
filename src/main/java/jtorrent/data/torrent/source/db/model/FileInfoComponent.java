package jtorrent.data.torrent.source.db.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Formula;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
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

    @OrderColumn
    @ElementCollection(fetch = FetchType.EAGER)
    private final List<byte[]> pieceHashes;

    @Column(nullable = false)
    private final int pieceSize;

    @Formula("infoHash")
    private final byte[] infoHash;

    protected FileInfoComponent() {
        this(null, Collections.emptyList(), Collections.emptyList(), 0, null);
    }

    public FileInfoComponent(String directory, List<FileMetadataComponent> fileMetadata, List<byte[]> pieceHashes,
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
        List<byte[]> pieceHashes = singleFileInfo.getPieceHashes().stream()
                .map(Sha1Hash::getBytes)
                .toList();
        int pieceSize = singleFileInfo.getPieceSize();
        byte[] infoHash = singleFileInfo.getInfoHash().getBytes();
        return new FileInfoComponent(null, fileMetadata, pieceHashes, pieceSize, infoHash);
    }

    public static FileInfoComponent fromDomain(MultiFileInfo multiFileInfo) {
        String directory = multiFileInfo.getDirectory();
        List<FileMetadataComponent> fileMetadata = multiFileInfo.getFileMetaData().stream()
                .map(FileMetadataComponent::fromDomain)
                .toList();
        List<byte[]> pieceHashes = multiFileInfo.getPieceHashes().stream()
                .map(Sha1Hash::getBytes)
                .toList();
        int pieceSize = multiFileInfo.getPieceSize();
        byte[] infoHash = multiFileInfo.getInfoHash().getBytes();
        return new FileInfoComponent(directory, fileMetadata, pieceHashes, pieceSize, infoHash);
    }

    public FileInfo toDomain() {
        return isSingleFile() ? toSingleFileInfo() : toMultiFileInfo();
    }

    private boolean isSingleFile() {
        return directory == null;
    }

    private SingleFileInfo toSingleFileInfo() {
        FileMetadata domainFileMetadata = fileMetadata.get(0).toDomain();
        List<Sha1Hash> domainPieceHashes = pieceHashes.stream()
                .map(Sha1Hash::new)
                .toList();
        Sha1Hash domainInfoHash = new Sha1Hash(infoHash);
        return new SingleFileInfo(domainFileMetadata, pieceSize, domainPieceHashes, domainInfoHash);
    }

    private MultiFileInfo toMultiFileInfo() {
        List<FileMetadata> domainFileMetadata = fileMetadata.stream()
                .map(FileMetadataComponent::toDomain)
                .toList();
        List<Sha1Hash> domainPieceHashes = pieceHashes.stream()
                .map(Sha1Hash::new)
                .toList();
        Sha1Hash domainInfoHash = new Sha1Hash(infoHash);
        return new MultiFileInfo(directory, domainFileMetadata, pieceSize, domainPieceHashes, domainInfoHash);
    }

    public String getDirectory() {
        return directory;
    }

    public List<FileMetadataComponent> getFileMetadata() {
        return fileMetadata;
    }

    public List<byte[]> getPieceHashes() {
        return pieceHashes;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    @Override
    public String toString() {
        return "FileInfoComponent{"
                + "directory='" + directory + '\''
                + ", fileMetadata=" + fileMetadata
                + ", pieceHashes=" + pieceHashes.stream().map(Arrays::toString).toList()
                + ", pieceSize=" + pieceSize
                + ", infoHash=" + Arrays.toString(infoHash)
                + '}';
    }
}
