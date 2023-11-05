package jtorrent.domain.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jtorrent.domain.util.exception.Sha1AlgorithmUnavailableException;

public class Sha1Hash extends Bit160Value {

    public static final int HASH_SIZE = 20;

    private static final MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new Sha1AlgorithmUnavailableException(e);
        }
    }

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
        byte[] hash = MESSAGE_DIGEST.digest(bytes);
        MESSAGE_DIGEST.reset();
        return new Sha1Hash(hash);
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

    @Override
    public String toString() {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }
}
