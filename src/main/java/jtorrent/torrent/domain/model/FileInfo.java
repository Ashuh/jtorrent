package jtorrent.torrent.domain.model;

import static jtorrent.common.domain.util.ValidationUtil.requireAtMost;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNegative;
import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import jtorrent.common.domain.util.Sha1Hash;

public abstract class FileInfo {

    private final List<FileWithPieceInfo> fileWithPieceInfos;
    private final List<Sha1Hash> pieceHashes;
    private final int pieceSize;
    private final long totalFileSize;
    private Path saveDirectory = Paths.get("download").toAbsolutePath(); // TODO: use default downloads folder?

    protected FileInfo(List<FileWithPieceInfo> fileWithPieceInfos, int pieceSize, long totalFileSize,
            List<Sha1Hash> pieceHashes) {
        this.fileWithPieceInfos = fileWithPieceInfos;
        this.pieceSize = pieceSize;
        this.totalFileSize = totalFileSize;
        this.pieceHashes = requireNonNull(pieceHashes);
    }

    protected static List<FileWithPieceInfo> build(List<File> files, int pieceSize) {
        List<FileWithPieceInfo> fileWithPieceInfos = new ArrayList<>();

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
            long firstPieceBytes = Math.min(pieceSize - firstPieceStart, file.getSize());
            long remainingFileBytes = file.getSize() - firstPieceBytes;
            int numRemainingPieces = (int) Math.ceil(remainingFileBytes / (double) pieceSize);
            int lastPiece = firstPiece + numRemainingPieces;
            long fileEnd = fileStart + file.getSize() - 1;
            int lastPieceEnd = (int) (fileEnd % pieceSize);

            prevLastPiece = lastPiece;
            prevLastPieceEnd = lastPieceEnd;
            FilePieceInfo filePieceInfo =
                    new FilePieceInfo(firstPiece, firstPieceStart, lastPiece, lastPieceEnd, fileStart, fileEnd);
            fileWithPieceInfos.add(new FileWithPieceInfo(file, filePieceInfo));
        }

        return fileWithPieceInfos;
    }

    /**
     * Returns a list of {@link FileWithPieceInfo} that fall within the given byte range.
     * The returned list is sorted by the start byte offset of the file.
     *
     * @param start the byte offset to start at (inclusive)
     * @param end   the byte offset to end at (inclusive)
     * @return a list of {@link FileWithPieceInfo} that fall within the given byte range
     */
    public List<FileWithPieceInfo> getInRange(long start, long end) {
        int startIndex = getFileIndex(start); // inclusive
        int endIndex = getFileIndex(end); // inclusive

        return IntStream.range(startIndex, endIndex + 1)
                .mapToObj(fileWithPieceInfos::get)
                .toList();
    }

    private int getFileIndex(long offset) {
        requireNonNegative(offset);
        requireAtMost(offset, totalFileSize - 1);

        int low = 0;
        int high = getNumFiles() - 1;

        while (low < high) {
            int mid = low + (high - low) / 2;
            FilePieceInfo midFilePieceInfo = getFileInfo(mid);
            long midStart = midFilePieceInfo.start();
            long midEnd = midFilePieceInfo.end();
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

    public int getNumFiles() {
        return fileWithPieceInfos.size();
    }

    public FilePieceInfo getFileInfo(int index) {
        return fileWithPieceInfos.get(index).filePieceInfo();
    }

    public abstract boolean isSingleFile();

    public List<FileWithPieceInfo> getFileWithInfos() {
        return fileWithPieceInfos;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public List<File> getFiles() {
        return fileWithPieceInfos.stream()
                .map(FileWithPieceInfo::file)
                .toList();
    }

    public List<Sha1Hash> getPieceHashes() {
        return pieceHashes;
    }

    public Sha1Hash getPieceHash(int index) {
        return pieceHashes.get(index);
    }

    public int getNumPieces() {
        return pieceHashes.size();
    }

    /**
     * Gets the {@link Path} to the directory that is used to save the torrent contents.
     * Torrent contents refer to either a single file in the case of a single file torrent,
     * or a directory in the case of a multi-file torrent.
     */
    public Path getSaveDirectory() {
        return saveDirectory;
    }

    public void setSaveDirectory(Path saveDirectory) {
        this.saveDirectory = requireNonNull(saveDirectory);
    }

    /**
     * Gets the root directory of the {@link File}s that this {@link FileInfo} contains.
     */
    public abstract Path getFileRoot();

    /**
     * Gets the path to the file or directory to which the torrent contents should be saved.
     * If the torrent is a single file torrent, then the returned path is the path to the file.
     * If the torrent is a multi-file torrent, then the returned path is the path to the directory containing the files.
     */
    public abstract Path getSaveAsPath();
}
