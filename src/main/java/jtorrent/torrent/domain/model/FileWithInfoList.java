package jtorrent.torrent.domain.model;

import static jtorrent.common.domain.util.ValidationUtil.requireAtMost;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNegative;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class FileWithInfoList {

    private final List<FileWithInfo> fileWithInfos;
    private final long totalFileSize;

    private FileWithInfoList(List<FileWithInfo> fileWithInfos, long totalFileSize) {
        this.fileWithInfos = fileWithInfos;
        this.totalFileSize = totalFileSize;
    }

    public static FileWithInfoList fromFilesWithPieceSize(List<File> files, long pieceSize) {
        List<FileWithInfo> fileWithInfos = new ArrayList<>();

        int prevLastPiece = 0; // inclusive
        int prevLastPieceEnd = -1; // inclusive

        for (File file : files) {
            int firstPiece = prevLastPiece;
            int firstPieceStart = prevLastPieceEnd + 1;
            boolean isPrevLastPieceFullyOccupied = firstPieceStart == pieceSize;
            if (isPrevLastPieceFullyOccupied) {
                firstPiece++;
                firstPieceStart = 0;
            }

            long fileStart = firstPiece * pieceSize + firstPieceStart;
            long firstPieceBytes = Math.min(file.getSize(), pieceSize) - firstPieceStart;
            long remainingFileBytes = file.getSize() - firstPieceBytes;
            int numRemainingPieces = (int) Math.ceil(remainingFileBytes / (double) pieceSize);
            int lastPiece = firstPiece + numRemainingPieces;
            int lastPieceEnd = (int) (remainingFileBytes % pieceSize) - 1;
            long fileEnd = lastPiece * pieceSize + lastPieceEnd;

            prevLastPiece = lastPiece;
            prevLastPieceEnd = lastPieceEnd;
            FileInfo fileInfo = new FileInfo(firstPiece, firstPieceStart, lastPiece, lastPieceEnd, fileStart, fileEnd);
            fileWithInfos.add(new FileWithInfo(file, fileInfo));
        }

        long totalFileSize = files.stream()
                .mapToLong(File::getSize)
                .sum();
        return new FileWithInfoList(fileWithInfos, totalFileSize);
    }

    public List<FileWithInfo> get() {
        return fileWithInfos;
    }

    public FileWithInfo get(int index) {
        return fileWithInfos.get(index);
    }

    /**
     * Returns a list of {@link FileWithInfo} that fall within the given byte range.
     * The returned list is sorted by the start byte offset of the file.
     *
     * @param start the byte offset to start at (inclusive)
     * @param end   the byte offset to end at (inclusive)
     * @return a list of {@link FileWithInfo} that fall within the given byte range
     */
    public List<FileWithInfo> getInRange(long start, long end) {
        int startIndex = getFileIndex(start); // inclusive
        int endIndex = getFileIndex(end); // inclusive

        return IntStream.range(startIndex, endIndex + 1)
                .mapToObj(this::get)
                .toList();
    }

    private int getFileIndex(long offset) {
        requireNonNegative(offset);
        requireAtMost(offset, totalFileSize - 1);

        int low = 0;
        int high = size() - 1;

        while (low < high) {
            int mid = low + (high - low) / 2;
            FileInfo midFileInfo = getFileInfo(mid);
            long midStart = midFileInfo.start();
            long midEnd = midFileInfo.end();
            boolean isOffsetWithinMidFile = offset >= midStart && offset <= midEnd;

            if (isOffsetWithinMidFile) {
                return mid;
            } else if (offset < midStart) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return high;
    }

    public FileInfo getFileInfo(int index) {
        return get(index).fileInfo();
    }

    public int size() {
        return fileWithInfos.size();
    }

    public File getFile(int index) {
        return get(index).file();
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public List<File> getFiles() {
        return fileWithInfos.stream()
                .map(FileWithInfo::file)
                .toList();
    }
}
