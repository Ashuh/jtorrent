package jtorrent.data.torrent.repository;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.repository.PieceRepository;

public class AppPieceRepository implements PieceRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppPieceRepository.class);
    private static final String READ_ONLY_MODE = "r";
    private static final String READ_WRITE_MODE = "rw";

    @Override
    public byte[] getPiece(Torrent torrent, int index) throws IOException {
        LOGGER.debug(Markers.TORRENT, "Getting piece {}", index);
        long start = torrent.getPieceOffset(index);
        int length = torrent.getPieceSize(index);
        return getData(torrent, start, length);
    }

    @Override
    public byte[] getBlock(Torrent torrent, int index, int offset, int length) throws IOException {
        LOGGER.debug(Markers.TORRENT, "Getting block in piece {} at offset {} with length {}", index, offset, length);
        long start = torrent.getPieceOffset(index) + offset;
        return getData(torrent, start, length);
    }

    private byte[] getData(Torrent torrent, long start, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        long end = start + length - 1; // inclusive
        LOGGER.debug(Markers.TORRENT, "Getting data in range [{}, {}]", start, end);

        List<FileMetadata> fileMetadata = torrent.getFileMetadataInRange(start, end);
        for (FileMetadata metadataItem : fileMetadata) {
            long startOffsetInFile = Math.max(start - metadataItem.start(), 0);
            long endOffsetInFile = Math.min(metadataItem.end(), end) - metadataItem.start();
            int readLength = (int) (endOffsetInFile - startOffsetInFile + 1);
            Path path = torrent.getRootSaveDirectory().resolve(metadataItem.path());
            buffer.put(read(path, startOffsetInFile, readLength));
        }
        return buffer.array();
    }

    @Override
    public void storeBlock(Torrent torrent, int index, int offset, byte[] data) throws IOException {
        long start = torrent.getPieceOffset(index) + offset;
        long end = start + data.length - 1; // inclusive
        LOGGER.debug(Markers.TORRENT, "Storing data in range [{}, {}]", start, end);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        List<FileMetadata> fileMetadata = torrent.getFileMetadataInRange(start, end);
        for (FileMetadata metadataItem : fileMetadata) {
            long startOffsetInFile = Math.max(start - metadataItem.start(), 0);
            long endOffsetInFile = Math.min(metadataItem.end(), end) - metadataItem.start();
            int writeLength = (int) (endOffsetInFile - startOffsetInFile + 1);
            byte[] fileData = new byte[writeLength];
            buffer.get(fileData);
            Path path = torrent.getRootSaveDirectory().resolve(metadataItem.path());
            write(path, startOffsetInFile, fileData);
        }
    }

    private byte[] read(Path path, long start, int length) throws IOException {
        LOGGER.trace(Markers.TORRENT, "Reading {} bytes from {} starting at {}", length, path, start);
        byte[] buffer = new byte[length];
        Files.createDirectories(path.getParent());
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), READ_ONLY_MODE)) {
            file.seek(start);
            file.read(buffer);
        }
        return buffer;
    }

    private void write(Path path, long start, byte[] data) throws IOException {
        LOGGER.trace(Markers.TORRENT, "Writing {} bytes to {} starting at {}", data.length, path, start);
        Files.createDirectories(path.getParent());
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), READ_WRITE_MODE)) {
            file.seek(start);
            file.write(data);
        }
    }
}
