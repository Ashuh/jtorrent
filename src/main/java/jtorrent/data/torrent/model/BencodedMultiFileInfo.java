package jtorrent.data.torrent.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jtorrent.data.torrent.model.util.MapUtil;
import jtorrent.domain.common.util.ContinuousMergedInputStream;
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

    public static BencodedMultiFileInfo fromPath(Path source, int pieceSize) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must be a directory");
        }

        List<Path> filePaths = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile).forEach(filePaths::add);
        }

        List<InputStream> inputStreams = new ArrayList<>();
        List<BencodedFile> files = new ArrayList<>();
        for (Path filePath : filePaths) {
            long length = Files.size(filePath);
            files.add(BencodedFile.fromPath(filePath, length));
            inputStreams.add(Files.newInputStream(filePath));
        }

        byte[] hashes = computeHashes(new ContinuousMergedInputStream(inputStreams), pieceSize);
        String dirName = source.getFileName().toString();
        return new BencodedMultiFileInfo(pieceSize, hashes, dirName, files);
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
        return new MultiFileInfo(name, fileMetaData, pieceLength, getDomainPieceHashes());
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
            FileMetadata fileMetadataItem = new FileMetadata(fileSize, filePath, firstPiece,
                    firstPieceStart, lastPiece, lastPieceEnd, fileStart, fileEnd);
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
