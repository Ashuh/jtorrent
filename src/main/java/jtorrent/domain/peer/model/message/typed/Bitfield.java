package jtorrent.domain.peer.model.message.typed;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.util.BitSet;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * The bitfield message may only be sent immediately after the handshaking sequence is completed, and before any other
 * messages are sent. It is optional, and need not be sent if a client has no pieces.
 * <p>
 * The bitfield message is variable length, where X is the length of the bitfield. The payload is a bitfield
 * representing the pieces that have been successfully downloaded. The first byte of the bitfield corresponds
 * to indices 0 - 7 from high bit to low bit. The next one 8-15, etc.
 * <p>
 * Bits that are cleared indicated a missing piece, and set bits indicate a valid and available
 * piece. Spare bits at the end are set to zero. The last byte should be extended to the right if necessary, i.e.
 * if the length of the bitfield is not a multiple of 8 bits, the value of the low bits in the last byte should be
 * zero.
 * <p>
 * <table>
 *     <caption>Payload Structure</caption>
 *     <tr>
 *         <th style="text-align: center">Field No.</th>
 *         <th style="text-align: center">Field Name</th>
 *         <th style="text-align: center">Size (Bytes)</th>
 *         <th style="text-align: center">Description</th>
 *         <th style="text-align: center">Position</th>
 *     </tr>
 *     <tr>
 *         <td style="text-align: center">1</td>
 *         <td style="text-align: center">bitfield</td>
 *         <td style="text-align: center">X</td>
 *         <td style="text-align: center">bitfield representing pieces that have been successfully downloaded</td>
 *         <td style="text-align: center">5</td>
 *     </tr>
 * </table>
 * Refer to {@link TypedPeerMessage} for the common header structure.
 *
 * @see <a href="http://www.bittorrent.org/beps/bep_0003.html#peer-messages">
 * BEP 3 - The BitTorrent Protocol Specification - Peer Messages</a>
 * @see <a href="https://wiki.theory.org/BitTorrentSpecification">BitTorrentSpecification - TheoryOrg</a>
 */
public class Bitfield extends TypedPeerMessage {

    /**
     * The bitset representing the pieces that are available.
     */
    private final BitSet bitSet;
    /**
     * The number of bytes required to represent all the pieces of the torrent as a bitfield.
     * This is equal to the number of pieces rounded up to the nearest multiple of 8.
     */
    private final int numBytesToPack;

    private Bitfield(BitSet bitSet, int numBytesToPack) {
        this.bitSet = (BitSet) requireNonNull(bitSet).clone();
        this.numBytesToPack = numBytesToPack;
    }

    /**
     * Creates a new {@link Bitfield} from the given bitset and the number of total pieces.
     *
     * @param bitSet         the bitset representing the pieces that are available
     * @param numTotalPieces the total number of pieces, must be greater than or equal to the length of the bitset
     * @return a new {@link Bitfield} from the given bitset and the number of total pieces
     */
    public static Bitfield fromBitSetAndNumTotalPieces(BitSet bitSet, int numTotalPieces) {
        if (bitSet.length() > numTotalPieces) {
            throw new IllegalArgumentException("BitSet length is greater than the number of total pieces");
        }
        int numBytesToPack = roundUpToNearestByte(numTotalPieces);
        return new Bitfield(bitSet, numBytesToPack);
    }

    /**
     * Rounds up the given value to the nearest multiple of 8.
     * For example, 9 would be rounded up to 16, 16 would be rounded up to 16, 17 would be rounded up to 24.
     *
     * @param value the value to round up
     * @return the value rounded up to the nearest multiple of 8
     */
    private static int roundUpToNearestByte(int value) {
        return (value + Byte.SIZE - 1) / Byte.SIZE;
    }

    public static Bitfield unpack(byte[] payload) {
        BitSet bitSet = new BitSet();

        for (int i = 0; i < payload.length; i++) {
            byte b = payload[i];
            int startIndex = i * Byte.SIZE;
            for (int j = 0; j < Byte.SIZE; j++) {
                if (isBitSet(b, j)) {
                    bitSet.set(startIndex + j);
                }
            }
        }

        return new Bitfield(bitSet, payload.length);
    }

    /**
     * Checks whether the bit at the specified index is set.
     *
     * @param b        the byte to check
     * @param bitIndex the index of the bit to check. 0 is the leftmost bit.
     * @return true if the bit is set, false otherwise
     */
    private static boolean isBitSet(byte b, int bitIndex) {
        int mask = 1 << (Byte.SIZE - 1 - bitIndex);
        return (b & mask) != 0;
    }

    public IntStream getBits() {
        return bitSet.stream();
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Bitfield bitfield = (Bitfield) o;
        return bitSet.equals(bitfield.bitSet);
    }

    @Override
    public int getPayloadSize() {
        return numBytesToPack;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.BITFIELD;
    }

    @Override
    protected byte[] getPayload() {
        byte[] bytes = new byte[numBytesToPack];
        bitSet.stream().forEach(i -> {
            int byteIndex = i / Byte.SIZE;
            int bitIndex = i % Byte.SIZE;
            int mask = 1 << (Byte.SIZE - 1 - bitIndex);
            bytes[byteIndex] |= (byte) mask;
        });
        return bytes;
    }

    @Override
    protected String getPayloadString() {
        StringBuilder stringBuilder = new StringBuilder();

        boolean inRange = false;
        int startRange = -1;

        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            if (!inRange) {
                // Start of a new range
                startRange = i;
                inRange = true;
            }

            // Check if the next bit is not set or if we have reached the end
            if (i + 1 >= bitSet.length() || !bitSet.get(i + 1)) {
                // End of the current range
                if (startRange == i) {
                    stringBuilder.append(i);
                } else {
                    stringBuilder.append(startRange).append("-").append(i);
                }

                // If it's not the last element, add a comma
                if (i + 1 < bitSet.length()) {
                    stringBuilder.append(",");
                }

                inRange = false;
            }
        }

        return stringBuilder.toString();
    }
}
