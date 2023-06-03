package jtorrent.domain.model.tracker.udp;

import static jtorrent.domain.Constants.PEER_ID;
import static jtorrent.domain.Constants.PORT;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.Event;
import jtorrent.domain.model.tracker.Tracker;
import jtorrent.domain.model.tracker.udp.message.Action;
import jtorrent.domain.model.tracker.udp.message.AnnounceRequest;
import jtorrent.domain.model.tracker.udp.message.ConnectionRequest;
import jtorrent.domain.model.tracker.udp.message.ConnectionResponse;
import jtorrent.domain.model.tracker.udp.message.ErrorResponse;
import jtorrent.domain.model.tracker.udp.message.Request;
import jtorrent.domain.model.tracker.udp.message.UdpAnnounceResponse;

public class UdpTracker extends Tracker {

    private static final System.Logger LOGGER = System.getLogger(UdpTracker.class.getName());
    private static final int UDP_MAX_PACKET_SIZE = 65536;
    private static final int CONNECTION_ID_EXPIRATION_MINS = 1;
    private static final String PROTOCOL = "udp";

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
    public UdpAnnounceResponse announce(Torrent torrent, Event event) throws IOException {
        // TODO: what to set for ipv4, key, numWant?
        AnnounceRequest announceRequest = new AnnounceRequest(connectionId,
                torrent.getInfoHash(),
                PEER_ID.getBytes(),
                torrent.getDownloaded(),
                torrent.getLeft(),
                torrent.getUploaded(),
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

        LOGGER.log(Level.DEBUG, "Received announce response: {0}", udpAnnounceResponse);
        return udpAnnounceResponse;
    }

    public void connect() throws IOException {
        LOGGER.log(Level.TRACE, "Getting connection ID");
        ConnectionRequest connectionRequest = new ConnectionRequest();
        sendRequest(connectionRequest);
        ConnectionResponse connectionResponse = receiveConnectionResponse();
        if (!connectionResponse.hasMatchingTransactionId(connectionRequest)) {
            throw new IOException("Transaction ID mismatch");
        }
        connectionId = connectionResponse.getConnectionId();
        connectionIdExpiration = LocalDateTime.now().plusMinutes(CONNECTION_ID_EXPIRATION_MINS);
        LOGGER.log(Level.DEBUG, "Received connection ID: " + connectionId);
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

    public void sendRequest(Request request) throws IOException {
        LOGGER.log(Level.DEBUG, "Sending request: {}", request);
        byte[] requestBytes = request.pack();
        DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length);
        socket.send(requestPacket);
    }

    public ConnectionResponse receiveConnectionResponse() throws IOException {
        LOGGER.log(Level.TRACE, "Waiting for connection response");
        DatagramPacket packet = new DatagramPacket(new byte[UDP_MAX_PACKET_SIZE], UDP_MAX_PACKET_SIZE);
        socket.receive(packet);

        if (packet.getLength() != ConnectionResponse.MESSAGE_BYTES) {
            throw new IOException("Invalid response length");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        Action action = Action.fromValue(buffer.getInt());
        byte[] payload = new byte[packet.getLength() - Integer.BYTES];
        buffer.get(payload);

        if (action != Action.CONNECT) {
            if (action == Action.ERROR) {
                ErrorResponse errorResponse = ErrorResponse.unpack(payload);
                throw new IOException(errorResponse.getMessage());
            }
            throw new IOException("Invalid action");
        }

        LOGGER.log(Level.TRACE, "Received connection response");
        return ConnectionResponse.unpack(payload);
    }

    public UdpAnnounceResponse receiveAnnounceResponse() throws IOException {
        LOGGER.log(Level.TRACE, "Waiting for announce response");
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
                ErrorResponse errorResponse = ErrorResponse.unpack(payload);
                throw new IOException(errorResponse.getMessage());
            }
            throw new IOException("Invalid action");
        }

        LOGGER.log(Level.TRACE, "Received announce response");
        return UdpAnnounceResponse.unpack(payload);
    }
}
