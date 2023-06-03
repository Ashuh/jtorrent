package jtorrent.data.repository;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.stream.IntStream;

import jtorrent.domain.model.torrent.File;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.repository.PieceRepository;
import jtorrent.domain.util.RangeList;

public class FilePieceRepository implements PieceRepository {

    private static final System.Logger LOGGER = System.getLogger(FilePieceRepository.class.getName());
    private static final String READ_ONLY_MODE = "r";
    private static final String READ_WRITE_MODE = "rw";

    @Override
    public byte[] getPiece(Torrent torrent, int index) {
        LOGGER.log(Level.DEBUG, "Getting piece {0} for torrent {1}", index, torrent.getName());
        int start = torrent.getPieceOffset(index);
        int length = torrent.getPieceSize(index);
        int end = start + length; // exclusive

        RangeList fileOffsetList = torrent.getFileByteRanges();
        int startIndex = fileOffsetList.getRangeIndex(start);
        int endIndex = fileOffsetList.getRangeIndex(end - 1);

        ByteBuffer buffer = ByteBuffer.allocate(length);

        IntStream.range(startIndex, endIndex + 1)
                .forEach(i -> {
                    File file = torrent.getFiles().get(i);
                    long fileStart = fileOffsetList.getRangeStart(i);
                    long fileEnd = fileOffsetList.getRangeEnd(i); // exclusive
                    long offsetInFile = Math.max(start - fileStart, 0);
                    int lengthToRead = (int) (Math.min(fileEnd, end) - (fileStart + offsetInFile));

                    try {
                        buffer.put(read(file.getPath(), offsetInFile, lengthToRead));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        return buffer.array();
    }

    @Override
    public void storeBlock(Torrent torrent, int index, int offset, byte[] data) {
        LOGGER.log(Level.DEBUG, "Storing block at index {0}, offset {1}, length {2} for torrent {3}",
                index, offset, data.length, torrent.getName());
        int start = torrent.getPieceOffset(index) + offset;
        int length = data.length;
        int end = start + length; // exclusive

        RangeList fileOffsetList = torrent.getFileByteRanges();
        int startIndex = fileOffsetList.getRangeIndex(start);
        int endIndex = fileOffsetList.getRangeIndex(end - 1);

        ByteBuffer buffer = ByteBuffer.wrap(data);

        IntStream.range(startIndex, endIndex + 1)
                .forEach(i -> {
                    File file = torrent.getFiles().get(i);
                    long fileStart = fileOffsetList.getRangeStart(i);
                    long fileEnd = fileOffsetList.getRangeEnd(i); // exclusive
                    long offsetInFile = Math.max(start - fileStart, 0);
                    int writeLength = (int) (Math.min(fileEnd, end) - (fileStart + offsetInFile));

                    try {
                        byte[] fileData = new byte[writeLength];
                        buffer.get(fileData);
                        write(file.getPath(), offsetInFile, fileData);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
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
