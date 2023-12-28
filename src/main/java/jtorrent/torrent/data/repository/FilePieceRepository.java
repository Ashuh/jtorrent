package jtorrent.torrent.data.repository;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import jtorrent.torrent.domain.model.File;
import jtorrent.torrent.domain.model.FileInfo;
import jtorrent.torrent.domain.model.Torrent;
import jtorrent.torrent.domain.repository.PieceRepository;

public class FilePieceRepository implements PieceRepository {

    private static final System.Logger LOGGER = System.getLogger(FilePieceRepository.class.getName());
    private static final String READ_ONLY_MODE = "r";
    private static final String READ_WRITE_MODE = "rw";

    @Override
    public byte[] getPiece(Torrent torrent, int index) throws IOException {
        LOGGER.log(Level.DEBUG, "[{0}] Getting piece {1}", torrent.getName(), index);
        long start = torrent.getPieceOffset(index);
        int length = torrent.getPieceSize(index);
        return getData(torrent, start, length);
    }

    @Override
    public byte[] getBlock(Torrent torrent, int index, int offset, int length) throws IOException {
        LOGGER.log(Level.DEBUG, "[{0}] Getting block at index {1}, offset {2}, length {3}",
                torrent.getName(), index, offset, length);
        long start = torrent.getPieceOffset(index) + offset;
        return getData(torrent, start, length);
    }

    private byte[] getData(Torrent torrent, long start, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        long end = start + length - 1; // inclusive

        List<Map.Entry<File, FileInfo>> files = torrent.getFilesInRange(start, end);
        for (Map.Entry<File, FileInfo> entry : files) {
            File file = entry.getKey();
            FileInfo fileInfo = entry.getValue();
            long startOffsetInFile = Math.max(start - fileInfo.start(), 0);
            long endOffsetInFile = Math.min(fileInfo.end(), end) - fileInfo.start();
            int readLength = (int) (endOffsetInFile - startOffsetInFile + 1);
            buffer.put(read(file.getPath(), startOffsetInFile, readLength));
        }
        return buffer.array();
    }

    @Override
    public void storeBlock(Torrent torrent, int index, int offset, byte[] data) throws IOException {
        LOGGER.log(Level.DEBUG, "Storing block at index {0}, offset {1}, length {2} for torrent {3}",
                index, offset, data.length, torrent.getName());
        long start = torrent.getPieceOffset(index) + offset;
        long end = start + data.length - 1; // inclusive

        ByteBuffer buffer = ByteBuffer.wrap(data);
        List<Map.Entry<File, FileInfo>> files = torrent.getFilesInRange(start, end);
        for (Map.Entry<File, FileInfo> entry : files) {
            File file = entry.getKey();
            FileInfo fileInfo = entry.getValue();
            long startOffsetInFile = Math.max(start - fileInfo.start(), 0);
            long endOffsetInFile = Math.min(fileInfo.end(), end) - fileInfo.start();
            int writeLength = (int) (endOffsetInFile - startOffsetInFile + 1);
            byte[] fileData = new byte[writeLength];
            buffer.get(fileData);
            write(file.getPath(), startOffsetInFile, fileData);
        }
    }

    private byte[] read(Path path, long start, int length) throws IOException {
        LOGGER.log(Level.DEBUG, "Reading {0} bytes from {1} starting at {2}", length, path, start);
        byte[] buffer = new byte[length];
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), READ_ONLY_MODE)) {
            file.seek(start);
            file.read(buffer);
        }
        return buffer;
    }

    private void write(Path path, long start, byte[] data) throws IOException {
        LOGGER.log(Level.DEBUG, "Writing {0} bytes to {1} starting at {2}", data.length, path, start);
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), READ_WRITE_MODE)) {
            file.seek(start);
            file.write(data);
        }
    }
}
