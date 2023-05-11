package jtorrent.domain.model.peer.message;

import static java.util.Objects.requireNonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

import jtorrent.domain.model.exception.UnpackException;
import jtorrent.domain.model.torrent.Sha1Hash;

public class Handshake implements PeerMessage {

    public static final int BYTES = 68;
    private static final String PROTOCOL_IDENTIFIER = "BitTorrent protocol";
    private static final byte PROTOCOL_IDENTIFIER_LENGTH = (byte) PROTOCOL_IDENTIFIER.length();

    private final Sha1Hash infoHash;
    private final byte[] peerId;
    private final byte[] flags = new byte[8];

    public Handshake(Sha1Hash infoHash, byte[] peerId) {
        if (peerId.length != 20) {
            throw new IllegalArgumentException("Peer ID must be 20 bytes long");
        }
        this.infoHash = requireNonNull(infoHash);
        this.peerId = requireNonNull(peerId);
    }

    public static Handshake unpack(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte length = buffer.get();

        if (length != PROTOCOL_IDENTIFIER_LENGTH) {
            throw new UnpackException("Invalid protocol identifier length: " + length);
        }

        byte[] identifierBytes = new byte[PROTOCOL_IDENTIFIER_LENGTH];
        buffer.get(identifierBytes);
        String identifier = new String(identifierBytes);

        if (!identifier.equals(PROTOCOL_IDENTIFIER)) {
            throw new UnpackException("Invalid protocol identifier: " + identifier);
        }

        byte[] flags = new byte[8];
        buffer.get(flags);

        byte[] infoHashBytes = new byte[20];
        buffer.get(infoHashBytes);
        Sha1Hash infoHash = new Sha1Hash(infoHashBytes);

        byte[] peerId = new byte[20];
        buffer.get(peerId);

        return new Handshake(infoHash, peerId);
    }

    public Sha1Hash getInfoHash() {
        return infoHash;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public byte[] getFlags() {
        return flags;
    }

    @Override
    public byte[] pack() {
        return ByteBuffer.allocate(BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .put(PROTOCOL_IDENTIFIER_LENGTH)
                .put(PROTOCOL_IDENTIFIER.getBytes())
                .put(flags)
                .put(infoHash.getBytes())
                .put(peerId)
                .array();
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(infoHash);
        result = 31 * result + Arrays.hashCode(peerId);
        result = 31 * result + Arrays.hashCode(flags);
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
        Handshake handshake = (Handshake) o;
        return infoHash.equals(handshake.infoHash)
                && Arrays.equals(peerId, handshake.peerId)
                && Arrays.equals(flags, handshake.flags);
    }

    @Override
    public String toString() {
        return "Handshake{" +
                "infoHash=" + infoHash +
                ", peerId=" + Arrays.toString(peerId) +
                ", flags=" + Arrays.toString(flags) +
                '}';
    }
}
