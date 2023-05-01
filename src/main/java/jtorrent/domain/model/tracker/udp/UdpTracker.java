package jtorrent.domain.model.tracker.udp;

import static jtorrent.domain.Constants.PEER_ID;
import static jtorrent.domain.Constants.PORT;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.List;

import jtorrent.domain.model.torrent.Sha1Hash;
import jtorrent.domain.model.tracker.Tracker;
import jtorrent.domain.model.tracker.udp.message.Action;
import jtorrent.domain.model.tracker.udp.message.AnnounceRequest;
import jtorrent.domain.model.tracker.udp.message.AnnounceResponse;
import jtorrent.domain.model.tracker.udp.message.ConnectionRequest;
import jtorrent.domain.model.tracker.udp.message.ConnectionResponse;
import jtorrent.domain.model.tracker.udp.message.ErrorResponse;
import jtorrent.domain.model.tracker.udp.message.Event;
import jtorrent.domain.model.tracker.udp.message.PeerResponse;
import jtorrent.domain.model.tracker.udp.message.Request;

public class UdpTracker extends Tracker {

    private static final int UDP_RECEIVE_TIMEOUT = 15000;
    private static final int UDP_MAX_PACKET_SIZE = 65536;

    private final InetSocketAddress address;
    private final DatagramSocket socket;

    public UdpTracker(InetSocketAddress address) throws SocketException {
        this.address = address;
        socket = new DatagramSocket();
        socket.setSoTimeout(UDP_RECEIVE_TIMEOUT);
        socket.connect(address);
    }

    @Override
    public List<PeerResponse> getPeers(Sha1Hash infoHash) throws IOException {
        ConnectionRequest connectionRequest = new ConnectionRequest();
        sendRequest(connectionRequest);
        ConnectionResponse connectionResponse = receiveConnectionResponse();

        if (!connectionResponse.hasMatchingTransactionId(connectionRequest)) {
            throw new IOException("Transaction ID mismatch");
        }

        // TODO: populate fields with actual values
        AnnounceRequest announceRequest = new AnnounceRequest(
                connectionResponse.getConnectionId(),
                infoHash,
                PEER_ID.getBytes(),
                0,
                0,
                0,
                Event.NONE,
                0,
                0,
                -1,
                PORT
        );

        sendRequest(announceRequest);
        AnnounceResponse announceResponse = receiveAnnounceResponse();

        if (!announceResponse.hasMatchingTransactionId(announceRequest)) {
            throw new IOException("Transaction ID mismatch");
        }

        return announceResponse.getPeers();
    }

    private void sendRequest(Request request) throws IOException {
        byte[] requestBytes = request.pack();
        DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length);
        socket.send(requestPacket);
    }

    private ConnectionResponse receiveConnectionResponse() throws IOException {
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

        return ConnectionResponse.unpack(payload);
    }

    private AnnounceResponse receiveAnnounceResponse() throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[UDP_MAX_PACKET_SIZE], UDP_MAX_PACKET_SIZE);
        socket.receive(packet);

        if (packet.getLength() < AnnounceResponse.MESSAGE_MIN_BYTES) {
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

        return AnnounceResponse.unpack(payload);
    }
}
