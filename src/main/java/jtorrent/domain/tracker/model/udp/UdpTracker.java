package jtorrent.domain.tracker.model.udp;

import static jtorrent.domain.common.Constants.PEER_ID;
import static jtorrent.domain.common.Constants.PORT;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.tracker.model.Event;
import jtorrent.domain.tracker.model.Tracker;
import jtorrent.domain.tracker.model.udp.message.Action;
import jtorrent.domain.tracker.model.udp.message.request.UdpAnnounceRequest;
import jtorrent.domain.tracker.model.udp.message.request.UdpConnectionRequest;
import jtorrent.domain.tracker.model.udp.message.request.UdpRequest;
import jtorrent.domain.tracker.model.udp.message.response.UdpAnnounceResponse;
import jtorrent.domain.tracker.model.udp.message.response.UdpConnectionResponse;
import jtorrent.domain.tracker.model.udp.message.response.UdpErrorResponse;

public class UdpTracker implements Tracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UdpTracker.class);
    private static final int UDP_MAX_PACKET_SIZE = 65536;
    private static final int CONNECTION_ID_EXPIRATION_MINS = 1;

    private final InetSocketAddress address;
    private DatagramSocket socket;
    private Long connectionId;
    private LocalDateTime connectionIdExpiration;

    public UdpTracker(InetSocketAddress address) {
        this.address = address;
    }

    public void init() throws SocketException {
        this.socket = new DatagramSocket();
        this.socket.connect(address);
    }

    @Override
    public UdpAnnounceResponse announce(Sha1Hash infoHash, long downloaded, long left, long uploaded, Event event)
            throws IOException {
        // TODO: what to set for ipv4, key, numWant?
        UdpAnnounceRequest announceRequest = new UdpAnnounceRequest(connectionId,
                infoHash,
                PEER_ID.getBytes(),
                downloaded,
                left,
                uploaded,
                event,
                0,
                0,
                -1,
                PORT
        );

        sendRequest(announceRequest);
        UdpAnnounceResponse udpAnnounceResponse = receiveAnnounceResponse();

        if (!udpAnnounceResponse.hasMatchingTransactionId(announceRequest)) {
            throw new IOException("Transaction ID mismatch");
        }

        LOGGER.debug(Markers.TRACKER, "Received announce response: {}", udpAnnounceResponse);
        return udpAnnounceResponse;
    }

    @Override
    public URI getUri() {
        return URI.create("udp://" + address.getHostString() + ":" + address.getPort());
    }

    public void connect() throws IOException {
        LOGGER.debug(Markers.TRACKER, "Connecting to tracker");
        UdpConnectionRequest connectionRequest = new UdpConnectionRequest();
        sendRequest(connectionRequest);
        UdpConnectionResponse connectionResponse = receiveConnectionResponse();
        if (!connectionResponse.hasMatchingTransactionId(connectionRequest)) {
            throw new IOException("Transaction ID mismatch");
        }
        connectionId = connectionResponse.getConnectionId();
        connectionIdExpiration = LocalDateTime.now().plusMinutes(CONNECTION_ID_EXPIRATION_MINS);
        LOGGER.info(Markers.TRACKER, "Connected to tracker");
    }

    public void setTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    public boolean hasValidConnectionId() {
        if (connectionId == null) {
            return false;
        }
        assert connectionIdExpiration != null;
        return LocalDateTime.now().isBefore(connectionIdExpiration);
    }

    public void sendRequest(UdpRequest request) throws IOException {
        byte[] requestBytes = request.pack();
        DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length);
        socket.send(requestPacket);
        LOGGER.debug(Markers.TRACKER, "Sent request: {}", request);
    }

    public UdpConnectionResponse receiveConnectionResponse() throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[UDP_MAX_PACKET_SIZE], UDP_MAX_PACKET_SIZE);
        socket.receive(packet);

        if (packet.getLength() != UdpConnectionResponse.MESSAGE_BYTES) {
            throw new IOException("Invalid response length");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        Action action = Action.fromValue(buffer.getInt());
        byte[] payload = new byte[packet.getLength() - Integer.BYTES];
        buffer.get(payload);

        if (action != Action.CONNECT) {
            if (action == Action.ERROR) {
                UdpErrorResponse errorResponse = UdpErrorResponse.unpack(payload);
                throw new IOException(errorResponse.getMessage());
            }
            throw new IOException("Invalid action");
        }

        UdpConnectionResponse response = UdpConnectionResponse.unpack(payload);
        LOGGER.debug(Markers.TRACKER, "Received connection response: {}", response);
        return response;
    }

    public UdpAnnounceResponse receiveAnnounceResponse() throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[UDP_MAX_PACKET_SIZE], UDP_MAX_PACKET_SIZE);
        socket.receive(packet);

        if (packet.getLength() < UdpAnnounceResponse.MESSAGE_MIN_BYTES) {
            throw new IOException("Invalid response length");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        Action action = Action.fromValue(buffer.getInt());
        byte[] payload = new byte[packet.getLength() - Integer.BYTES];
        buffer.get(payload);

        if (action != Action.ANNOUNCE) {
            if (action == Action.ERROR) {
                UdpErrorResponse errorResponse = UdpErrorResponse.unpack(payload);
                throw new IOException(errorResponse.getMessage());
            }
            throw new IOException("Invalid action");
        }

        UdpAnnounceResponse response = UdpAnnounceResponse.unpack(payload);
        LOGGER.debug(Markers.TRACKER, "Received announce response: {}", response);
        return response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UdpTracker that = (UdpTracker) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
