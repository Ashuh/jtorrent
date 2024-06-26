package jtorrent.data.torrent.source.file.model;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jtorrent.data.torrent.source.file.model.util.MapUtil;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.MultiFileInfo;

public class BencodedMultiFileInfo extends BencodedInfo {

    private final List<BencodedFile> files;

    public BencodedMultiFileInfo(int pieceLength, byte[] pieces, String name, List<BencodedFile> files) {
        super(pieceLength, pieces, name);
        this.files = files;
    }

    public static BencodedMultiFileInfo fromMap(Map<String, Object> map) {
        int pieceLength = MapUtil.getValueAsLong(map, KEY_PIECE_LENGTH).orElseThrow().intValue();
        byte[] pieces = MapUtil.getValueAsByteArray(map, KEY_PIECES).orElseThrow();
        String name = MapUtil.getValueAsString(map, KEY_NAME).orElseThrow();
        List<Map<String, Object>> filesRaw = MapUtil.getValueAsList(map, KEY_FILES);
        List<BencodedFile> files = filesRaw.stream()
                .map(BencodedFile::fromMap)
                .collect(Collectors.toList());

        return new BencodedMultiFileInfo(pieceLength, pieces, name, files);
    }

    public static BencodedMultiFileInfo fromDomain(MultiFileInfo fileInfo) {
        byte[] pieces = Sha1Hash.concatHashes(fileInfo.getPieceHashes());
        int pieceLength = fileInfo.getPieceSize();
        String name = fileInfo.getName();
        List<BencodedFile> files = fileInfo.getFileMetaData().stream()
                .map(BencodedFile::fromDomain)
                .toList();
        return new BencodedMultiFileInfo(pieceLength, pieces, name, files);
    }

    @Override
    public List<BencodedFile> getFiles() {
        return files;
    }

    @Override
    public long getTotalSize() {
        return files.stream()
                .mapToLong(BencodedFile::getLength)
                .sum();
    }

    @Override
    public FileInfo toDomain() {
        List<FileMetadata> fileMetaData = buildFileMetaData();
        return new MultiFileInfo(name, fileMetaData, pieceLength, Sha1Hash.splitHashes(pieces),
                new Sha1Hash(getInfoHash()));
    }

    protected List<FileMetadata> buildFileMetaData() {
        List<FileMetadata> fileMetaData = new ArrayList<>();

        int prevLastPiece = 0; // inclusive
        int prevLastPieceEnd = -1; // inclusive

        for (BencodedFile file : getFiles()) {
            long fileSize = file.getLength();

            int firstPiece = prevLastPiece;
            int firstPieceStart = prevLastPieceEnd + 1;
            boolean isPrevLastPieceFullyOccupied = firstPieceStart == pieceLength;
            if (isPrevLastPieceFullyOccupied) {
                firstPiece++;
                firstPieceStart = 0;
            }

            long fileStart = firstPiece * pieceLength + firstPieceStart;
            long firstPieceBytes = Math.min(pieceLength - firstPieceStart, fileSize);
            long remainingFileBytes = fileSize - firstPieceBytes;
            int numRemainingPieces = (int) Math.ceil(remainingFileBytes / (double) pieceLength);
            int lastPiece = firstPiece + numRemainingPieces;
            long fileEnd = fileStart + fileSize - 1;
            int lastPieceEnd = (int) (fileEnd % pieceLength);

            prevLastPiece = lastPiece;
            prevLastPieceEnd = lastPieceEnd;

            Path filePath = Path.of(String.join("/", sanitizePath(file.getPath())));
            FileMetadata fileMetadataItem = new FileMetadata(filePath, fileStart, fileSize, firstPiece,
                    firstPieceStart, lastPiece, lastPieceEnd);
            fileMetaData.add(fileMetadataItem);
        }

        return fileMetaData;
    }

    protected static List<String> sanitizePath(List<String> path) {
        return path.stream()
                .map(part -> part.replaceAll("[\\\\/:*?\"<>|]", "_"))
                .toList();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), files);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        BencodedMultiFileInfo that = (BencodedMultiFileInfo) o;
        return Objects.equals(files, that.files);
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
                KEY_PIECE_LENGTH, pieceLength,
                KEY_PIECES, ByteBuffer.wrap(pieces),
                KEY_NAME, name,
                KEY_FILES, files.stream().map(BencodedFile::toMap).collect(Collectors.toList())
        );
    }

    @Override
    public String toString() {
        return "MultiFileInfo{"
                + "pieceLength=" + pieceLength
                + ", pieces=" + Arrays.toString(pieces)
                + ", name='" + name + '\''
                + ", files=" + files
                + '}';
    }
}
