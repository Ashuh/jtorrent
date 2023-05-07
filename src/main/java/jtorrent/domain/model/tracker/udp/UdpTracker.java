package jtorrent.domain.model.tracker.udp;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import jtorrent.domain.model.tracker.udp.message.Action;
import jtorrent.domain.model.tracker.udp.message.AnnounceResponse;
import jtorrent.domain.model.tracker.udp.message.ConnectionResponse;
import jtorrent.domain.model.tracker.udp.message.ErrorResponse;
import jtorrent.domain.model.tracker.udp.message.Request;

public class UdpTracker {

    private static final System.Logger LOGGER = System.getLogger(UdpTracker.class.getName());
    private static final int UDP_MAX_PACKET_SIZE = 65536;

    private final DatagramSocket socket;

    public UdpTracker(InetSocketAddress address) throws SocketException {
        this.socket = new DatagramSocket();
        this.socket.connect(address);
    }

    public void setTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
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

    public AnnounceResponse receiveAnnounceResponse() throws IOException {
        LOGGER.log(Level.TRACE, "Waiting for announce response");
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

        LOGGER.log(Level.TRACE, "Received announce response");
        return AnnounceResponse.unpack(payload);
    }
}
