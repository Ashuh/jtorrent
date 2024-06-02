package jtorrent.domain.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Sha1Hash extends Bit160Value {

    public static final int HASH_SIZE = 20;

    public Sha1Hash(byte[] hash) {
        super(hash);
    }

    /**
     * Computes the SHA-1 hash of the given bytes.
     *
     * @param bytes the bytes to hash
     * @return the SHA-1 hash of the given bytes
     */
    public static Sha1Hash of(byte[] bytes) {
        byte[] hash = getSha1MessageDigest().digest(bytes);
        return new Sha1Hash(hash);
    }

    /**
     * Creates and returns a new SHA-1 {@link MessageDigest}.
     * {@link MessageDigest}s are not reused because they are not thread-safe.
     *
     * @return a new SHA-1 {@link MessageDigest}
     */
    private static MessageDigest getSha1MessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static Sha1Hash fromHexString(String hexString) {
        if (hexString.length() != HASH_SIZE * 2) {
            throw new IllegalArgumentException("Invalid SHA1 hex string length");
        }

        byte[] hash = new byte[HASH_SIZE];
        for (int i = 0; i < HASH_SIZE; i++) {
            hash[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        return new Sha1Hash(hash);
    }

    public static List<Sha1Hash> splitHashes(byte[] hashesConcat) {
        if (hashesConcat.length % HASH_SIZE != 0) {
            throw new IllegalArgumentException("Invalid concatenated hashes length");
        }

        List<Sha1Hash> pieceHashes = new ArrayList<>();
        for (int i = 0; i < hashesConcat.length; i += Sha1Hash.HASH_SIZE) {
            byte[] pieceHash = new byte[Sha1Hash.HASH_SIZE];
            System.arraycopy(hashesConcat, i, pieceHash, 0, Sha1Hash.HASH_SIZE);
            pieceHashes.add(new Sha1Hash(pieceHash));
        }
        return pieceHashes;
    }

    public static byte[] concatHashes(List<Sha1Hash> hashes) {
        byte[] hashesConcat = new byte[hashes.size() * HASH_SIZE];
        for (int i = 0; i < hashes.size(); i++) {
            System.arraycopy(hashes.get(i).getBytes(), 0, hashesConcat, i * Sha1Hash.HASH_SIZE, Sha1Hash.HASH_SIZE);
        }
        return hashesConcat;
    }

    @Override
    public String toString() {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }

        return hexString.toString();
    }
}
