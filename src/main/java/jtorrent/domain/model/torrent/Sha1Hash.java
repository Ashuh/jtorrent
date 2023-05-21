package jtorrent.domain.model.torrent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import jtorrent.domain.model.torrent.exception.Sha1AlgorithmUnavailableException;

public class Sha1Hash {

    public static final int HASH_SIZE = 20;

    private static final MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new Sha1AlgorithmUnavailableException(e);
        }
    }

    private final byte[] bytes;

    public Sha1Hash(byte[] hash) {
        if (hash.length != HASH_SIZE) {
            throw new IllegalArgumentException("SHA1 hash must be 20 bytes long");
        }
        this.bytes = hash;
    }

    /**
     * Computes the SHA-1 hash of the given bytes.
     *
     * @param bytes the bytes to hash
     * @return the SHA-1 hash of the given bytes
     */
    public static Sha1Hash of(byte[] bytes) {
        byte[] hash = MESSAGE_DIGEST.digest(bytes);
        MESSAGE_DIGEST.reset();
        return new Sha1Hash(hash);
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Sha1Hash sha1Hash = (Sha1Hash) o;
        return Arrays.equals(bytes, sha1Hash.bytes);
    }

    @Override
    public String toString() {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }
}
