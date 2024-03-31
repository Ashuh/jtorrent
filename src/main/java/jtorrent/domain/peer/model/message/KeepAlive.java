package jtorrent.domain.peer.model.message;

import java.nio.ByteBuffer;

/**
 * The keep-alive message is a message containing only a length prefix set to zero.
 * There is no message ID and no payload. Peers may close a connection if they receive no messages
 * (keep-alive or any other message) for a certain period of time, so a keep-alive message must be
 * sent to maintain the connection alive if no command have been sent for a given amount of time.
 * This amount of time is generally two minutes.
 * <p>
 * <table>
 *     <caption>Message Structure</caption>
 *     <tr>
 *         <th style="text-align: center">Field No.</th>
 *         <th style="text-align: center">Field Name</th>
 *         <th style="text-align: center">Size (Bytes)</th>
 *         <th style="text-align: center">Description</th>
 *         <th style="text-align: center">Position</th>
 *     </tr>
 *     <tr>
 *         <td style="text-align: center">1</td>
 *         <td style="text-align: center">Length Prefix</td>
 *         <td style="text-align: center">4</td>
 *         <td style="text-align: center">
 *             length of message in bytes (excluding the length prefix itself).
 *             For the keep-alive message, this is always 0.
 *         </td>
 *         <td style="text-align: center">0</td>
 *     </tr>
 * </table>
 *
 * @see <a href="http://www.bittorrent.org/beps/bep_0003.html#peer-messages">
 * BEP 3 - The BitTorrent Protocol Specification - Peer Messages</a>
 * @see <a href="https://wiki.theory.org/BitTorrentSpecification">BitTorrentSpecification - TheoryOrg</a>
 */
public class KeepAlive implements PeerMessage {

    private static final int BYTES = 4;

    @Override
    public byte[] pack() {
        return ByteBuffer.allocate(BYTES)
                .putInt(0)
                .array();
    }

    @Override
    public int getMessageSize() {
        return BYTES;
    }

    @Override
    public String toString() {
        return "[KEEP ALIVE]";
    }
}
