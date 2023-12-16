package jtorrent.peer.domain.model.message.typed;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jtorrent.peer.domain.model.message.PeerMessage;

/**
 * The structure of all TypedPeerMessages consists of a header followed by a payload.
 * The table below shows the structure of the header common to all TypedPeerMessages.
 * <p>
 * <table>
 *     <caption>Header Structure</caption>
 *     <tr>
 *         <th style="text-align: center">Field No.</th>
 *         <th style="text-align: center">Field Name</th>
 *         <th style="text-align: center">Size (Bytes)</th>
 *         <th style="text-align: center">Description</th>
 *         <th style="text-align: center">Position</th>
 *     </tr>
 *     <tr>
 *         <td style="text-align: center">1</td>
 *
 *         <td style="text-align: center">Length Prefix</td>
 *         <td style="text-align: center">4</td>
 *         <td style="text-align: center">length of message in bytes (excluding the length prefix itself)</td>
 *         <td style="text-align: center">0</td>
 *     </tr>
 *     <tr>
 *         <td style="text-align: center">2</td>
 *         <td style="text-align: center">Message ID</td>
 *         <td style="text-align: center">1</td>
 *         <td style="text-align: center">message type code (See {@link MessageType})</td>
 *         <td style="text-align: center">4</td>
 *     </tr>
 * </table>
 *
 * @see <a href="http://www.bittorrent.org/beps/bep_0003.html#peer-messages">
 * BEP 3 - The BitTorrent Protocol Specification - Peer Messages</a>
 * @see <a href="https://wiki.theory.org/BitTorrentSpecification">BitTorrentSpecification - TheoryOrg</a>
 */
public abstract class TypedPeerMessage implements PeerMessage {

    private static final String TO_STRING_FORMAT_WITH_PAYLOAD = "[%s: [%s]]";
    private static final String TO_STRING_FORMAT_WITHOUT_PAYLOAD = "[%s]";
    private static final int LENGTH_PREFIX_SIZE = Integer.BYTES;
    private static final int MESSAGE_TYPE_SIZE = Byte.BYTES;
    /**
     * Size of the message header in bytes.
     * <p>
     * The header consists of a 4-byte length prefix indicating the length of the message in bytes
     * (excluding the length prefix itself) and a 1-byte message type code.
     */
    protected static final int HEADER_SIZE = LENGTH_PREFIX_SIZE + MESSAGE_TYPE_SIZE;

    public byte[] pack() {
        return ByteBuffer.allocate(getMessageSize())
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(getLengthPrefix())
                .put(getMessageType().getValue())
                .put(getPayload())
                .array();
    }

    @Override
    public int getMessageSize() {
        return getPayloadSize() + HEADER_SIZE;
    }

    /**
     * Gets the size of the payload in bytes.
     */
    protected abstract int getPayloadSize();

    private int getLengthPrefix() {
        return getPayloadSize() + MESSAGE_TYPE_SIZE;
    }

    public abstract MessageType getMessageType();

    protected abstract byte[] getPayload();

    @Override
    public final String toString() {
        String payloadString = getPayloadString();
        if (payloadString == null || payloadString.isEmpty()) {
            return String.format(TO_STRING_FORMAT_WITHOUT_PAYLOAD, getMessageType());
        }
        return String.format(TO_STRING_FORMAT_WITH_PAYLOAD, getMessageType(), getPayloadString());
    }

    protected abstract String getPayloadString();
}
