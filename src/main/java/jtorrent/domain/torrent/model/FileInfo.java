package jtorrent.domain.torrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireAtMost;
import static jtorrent.domain.common.util.ValidationUtil.requireNonNegative;
import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import jtorrent.domain.common.util.Sha1Hash;

public abstract class FileInfo {

    private static final int BLOCK_SIZE = 16384;

    protected final List<FileMetadata> fileMetaData;
    protected final List<Sha1Hash> pieceHashes;
    protected final int pieceSize;
    protected final Sha1Hash infoHash;

    protected FileInfo(List<FileMetadata> fileMetaData, int pieceSize, List<Sha1Hash> pieceHashes, Sha1Hash infoHash) {
        this.fileMetaData = requireNonNull(fileMetaData);
        this.pieceSize = pieceSize;
        this.pieceHashes = requireNonNull(pieceHashes);
        this.infoHash = requireNonNull(infoHash);
    }

    /**
     * Returns a list of {@link FileMetadataWithState} that fall within the given byte range.
     * The returned list is sorted by the start byte offset of the file.
     *
     * @param start the byte offset to start at (inclusive)
     * @param end   the byte offset to end at (inclusive)
     * @return a list of {@link FileMetadataWithState} that fall within the given byte range
     */
    public List<FileMetadata> getInRange(long start, long end) {
        int startIndex = getFileIndex(start); // inclusive
        int endIndex = getFileIndex(end); // inclusive

        return IntStream.range(startIndex, endIndex + 1)
                .mapToObj(fileMetaData::get)
                .toList();
    }

    private int getFileIndex(long offset) {
        requireNonNegative(offset);
        requireAtMost(offset, getTotalFileSize() - 1);

        int low = 0;
        int high = getNumFiles() - 1;

        while (low < high) {
            int mid = low + (high - low) / 2;
            FileMetadata metaData = fileMetaData.get(mid);
            long midStart = metaData.start();
            long midEnd = metaData.end();
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

    public int getBlockSize() {
        return BLOCK_SIZE;
    }

    public int getBlockSize(int pieceIndex, int blockIndex) {
        if (blockIndex == getNumBlocks(pieceIndex) - 1) {
            int remainder = getPieceSize(pieceIndex) % BLOCK_SIZE;
            return remainder == 0 ? BLOCK_SIZE : remainder;
        }
        return BLOCK_SIZE;
    }

    public int getNumBlocks(int pieceIndex) {
        return (int) Math.ceil((double) getPieceSize(pieceIndex) / BLOCK_SIZE);
    }

    public int getPieceSize(int piece) {
        if (piece < 0 || piece >= getNumPieces()) {
            throw new IllegalArgumentException("Invalid piece index: " + piece);
        }

        if (piece == getNumPieces() - 1) {
            int remainder = (int) (getTotalFileSize() % pieceSize);
            return remainder == 0 ? pieceSize : remainder;
        }

        return pieceSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public long getPieceOffset(int piece) {
        return (long) getPieceSize() * piece;
    }

    public int getNumFiles() {
        return fileMetaData.size();
    }

    public long getTotalFileSize() {
        return getFileMetaData().stream()
                .mapToLong(FileMetadata::size)
                .sum();
    }

    public List<FileMetadata> getFileMetaData() {
        return fileMetaData;
    }

    public FileMetadata getFileMetaData(Path path) {
        return fileMetaData.stream()
                .filter(file -> file.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));
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

    public Sha1Hash getInfoHash() {
        return infoHash;
    }

    /**
     * Gets the root directory of the files that this {@link FileInfo} contains.
     */
    public abstract Path getFileRoot();

    /**
     * Gets the name of the file or directory that the torrent contents should be saved as.
     *
     * @return the name of the file if the torrent is a single file torrent,
     * or the name of the directory if the torrent is a multi-file torrent
     */
    public abstract String getName();

    @Override
    public int hashCode() {
        return Objects.hash(fileMetaData, pieceHashes, pieceSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileInfo fileInfo = (FileInfo) o;
        return pieceSize == fileInfo.pieceSize
                && Objects.equals(fileMetaData, fileInfo.fileMetaData)
                && Objects.equals(pieceHashes, fileInfo.pieceHashes);
    }
}
