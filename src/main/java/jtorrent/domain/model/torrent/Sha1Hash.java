package jtorrent.domain.model.torrent;

import java.util.Arrays;

public class Sha1Hash {

    public static int HASH_SIZE = 20;

    private final byte[] bytes;

    public Sha1Hash(byte[] hash) {
        if (hash.length != 20) {
            throw new IllegalArgumentException("SHA1 hash must be 20 bytes long");
        }
        this.bytes = hash;
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
