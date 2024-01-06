package jtorrent.torrent.data.model;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.common.domain.util.bencode.BencodedObject;
import jtorrent.torrent.domain.model.FileInfo;

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

    protected List<Sha1Hash> getDomainPieceHashes() {
        List<Sha1Hash> pieceHashes = new ArrayList<>();
        for (int i = 0; i < pieces.length; i += Sha1Hash.HASH_SIZE) {
            byte[] pieceHash = new byte[Sha1Hash.HASH_SIZE];
            System.arraycopy(pieces, i, pieceHash, 0, Sha1Hash.HASH_SIZE);
            pieceHashes.add(new Sha1Hash(pieceHash));
        }
        return pieceHashes;
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

    public byte[] getInfoHash() throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(bencode());
    }

    public abstract List<BencodedFile> getFiles();

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
