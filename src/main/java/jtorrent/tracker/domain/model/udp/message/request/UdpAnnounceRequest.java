package jtorrent.tracker.domain.model.udp.message.request;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.tracker.domain.model.Event;
import jtorrent.tracker.domain.model.udp.message.Action;

/**
 * Represents an announce request.
 *
 * @see <a href="https://www.bittorrent.org/beps/bep_0015.html">UDP Tracker Protocol for BitTorrent</a>
 */
public class UdpAnnounceRequest extends UdpRequest {

    private static final int BYTES = 98;

    private final long connectionId;
    private final Sha1Hash infoHash;
    private final byte[] peerId;
    private final long downloaded;
    private final long left;
    private final long uploaded;
    private final Event event;
    private final int ipv4;
    private final int key;
    private final int numWant;
    private final int port; // unsigned short

    public UdpAnnounceRequest(long connectionId, Sha1Hash infoHash, byte[] peerId, long downloaded,
            long left, long uploaded, Event event, int ipv4, int key, int numWant, int port) {

        if (peerId.length != 20) {
            throw new IllegalArgumentException();
        }

        this.connectionId = connectionId;
        this.infoHash = requireNonNull(infoHash);
        this.peerId = peerId;
        this.downloaded = downloaded;
        this.left = left;
        this.uploaded = uploaded;
        this.event = requireNonNull(event);
        this.ipv4 = ipv4;
        this.key = key;
        this.numWant = numWant;
        this.port = port;
    }

    @Override
    public byte[] pack() {
        return ByteBuffer.allocate(BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(connectionId)
                .putInt(Action.ANNOUNCE.getValue())
                .putInt(transactionId)
                .put(infoHash.getBytes())
                .put(peerId)
                .putLong(downloaded)
                .putLong(left)
                .putLong(uploaded)
                .putInt(event.getUdpValue())
                .putInt(ipv4)
                .putInt(key)
                .putInt(numWant)
                .putShort((short) port)
                .array();
    }

    public long getConnectionId() {
        return connectionId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public Sha1Hash getInfoHash() {
        return infoHash;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public long getLeft() {
        return left;
    }

    public long getUploaded() {
        return uploaded;
    }

    public Event getEvent() {
        return event;
    }

    public int getIpv4() {
        return ipv4;
    }

    public int getKey() {
        return key;
    }

    public int getNumWant() {
        return numWant;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "AnnounceRequest{"
                + "connectionId=" + connectionId
                + ", infoHash=" + infoHash
                + ", peerId=" + Arrays.toString(peerId)
                + ", downloaded=" + downloaded
                + ", left=" + left
                + ", uploaded=" + uploaded
                + ", event=" + event
                + ", ipv4=" + ipv4
                + ", key=" + key
                + ", numWant=" + numWant
                + ", port=" + port
                + ", transactionId=" + transactionId
                + '}';
    }
}
