package jtorrent.data.torrent.source.file.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jtorrent.domain.common.util.ContinuousMergedInputStream;
import jtorrent.domain.common.util.Sha1Hash;

public class BencodedTorrentFactory {

    private BencodedTorrentFactory() {
    }

    public static BencodedTorrent create(Path source, List<List<String>> trackerUrls, String comment,
            String createdBy, int pieceSize) throws IOException {
        Long creationDate = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        BencodedInfo info = buildBencodedInfo(source, pieceSize);
        return BencodedTorrent.withAnnounceList(creationDate, trackerUrls, comment, createdBy, info);
    }

    private static BencodedInfo buildBencodedInfo(Path source, int pieceSize) throws IOException {
        if (Files.isDirectory(source)) {
            return buildBencodedMultiFileInfo(source, pieceSize);
        } else {
            return buildBencondedSingleFileInfo(source, pieceSize);
        }
    }

    private static BencodedSingleFileInfo buildBencondedSingleFileInfo(Path source, int pieceSize) throws IOException {
        if (Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must be a file");
        }

        byte[] hashes = computeHashes(Files.newInputStream(source), pieceSize);
        String fileName = source.getFileName().toString();
        long length = Files.size(source);
        return new BencodedSingleFileInfo(pieceSize, hashes, fileName, length);
    }

    private static BencodedMultiFileInfo buildBencodedMultiFileInfo(Path source, int pieceSize) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must be a directory");
        }

        List<Path> filePaths = getFilesInDirectory(source);

        List<InputStream> inputStreams = new ArrayList<>();
        List<BencodedFile> files = new ArrayList<>();
        for (Path filePath : filePaths) {
            long length = Files.size(filePath);
            files.add(BencodedFile.fromPath(source.relativize(filePath), length));
            inputStreams.add(Files.newInputStream(filePath));
        }

        byte[] hashes = computeHashes(new ContinuousMergedInputStream(inputStreams), pieceSize);
        String dirName = source.getFileName().toString();
        return new BencodedMultiFileInfo(pieceSize, hashes, dirName, files);
    }

    private static List<Path> getFilesInDirectory(Path directory) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    private static byte[] computeHashes(InputStream inputStream, int pieceSize) throws IOException {
        List<Sha1Hash> hashes = new ArrayList<>();
        int bytesRead;
        byte[] buffer = new byte[pieceSize];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byte[] piece = Arrays.copyOf(buffer, bytesRead);
            hashes.add(Sha1Hash.of(piece));
        }
        return Sha1Hash.concatHashes(hashes);
    }
}
