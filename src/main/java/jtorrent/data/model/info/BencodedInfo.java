package jtorrent.data.model.info;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jtorrent.data.model.BencodedObject;

public abstract class BencodedInfo extends BencodedObject {

    public static final String KEY_PIECE_LENGTH = "piece length";
    public static final String KEY_PIECES = "pieces";
    public static final String KEY_NAME = "name";
    public static final String KEY_LENGTH = "length";
    public static final String KEY_FILES = "files";

    protected final int pieceLength;
    protected final byte[] pieces;
    protected final String name;

    public BencodedInfo(int pieceLength, byte[] pieces, String name) {
        this.pieceLength = pieceLength;
        this.pieces = pieces;
        this.name = name;
    }

    public static BencodedInfo fromMap(Map<String, Object> map) {
        if (map.containsKey(KEY_LENGTH)) {
            return SingleFileInfo.fromMap(map);
        } else if (map.containsKey(KEY_FILES)) {
            return MultiFileInfo.fromMap(map);
        }

        throw new IllegalArgumentException("Invalid info dictionary");
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
