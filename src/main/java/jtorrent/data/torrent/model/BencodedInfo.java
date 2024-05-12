package jtorrent.data.torrent.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.bencode.BencodedObject;
import jtorrent.domain.torrent.model.FileInfo;

public abstract class BencodedInfo extends BencodedObject {

    public static final String KEY_PIECE_LENGTH = "piece length";
    public static final String KEY_PIECES = "pieces";
    public static final String KEY_NAME = "name";
    public static final String KEY_LENGTH = "length";
    public static final String KEY_FILES = "files";

    protected final int pieceLength;
    protected final byte[] pieces;
    /**
     * The name of the file or directory
     */
    protected final String name;

    protected BencodedInfo(int pieceLength, byte[] pieces, String name) {
        this.pieceLength = pieceLength;
        this.pieces = pieces;
        this.name = name;
    }

    public int getNumPieces() {
        return pieces.length / Sha1Hash.HASH_SIZE;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public byte[] getPieces() {
        return pieces;
    }

    public String getName() {
        return name;
    }

    public byte[] getInfoHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(bencode());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public abstract List<BencodedFile> getFiles();

    public abstract long getTotalSize();

    public abstract FileInfo toDomain();

    @Override
    public int hashCode() {
        int result = Objects.hash(pieceLength, name);
        result = 31 * result + Arrays.hashCode(pieces);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BencodedInfo info = (BencodedInfo) o;
        return pieceLength == info.pieceLength
                && Arrays.equals(pieces, info.pieces)
                && Objects.equals(name, info.name);
    }
}
