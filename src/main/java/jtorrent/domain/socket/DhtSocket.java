package jtorrent.domain.socket;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import jtorrent.domain.model.dht.message.DhtMessage;
import jtorrent.domain.model.dht.message.MessageType;
import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.error.Error;
import jtorrent.domain.model.dht.message.query.AnnouncePeer;
import jtorrent.domain.model.dht.message.query.FindNode;
import jtorrent.domain.model.dht.message.query.GetPeers;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.message.query.Ping;
import jtorrent.domain.model.dht.message.query.Query;
import jtorrent.domain.model.dht.message.response.DefinedResponse;
import jtorrent.domain.model.dht.message.response.UndefinedResponse;
import jtorrent.domain.util.bencode.BencodedMap;

public class DhtSocket {

    private static final Logger LOGGER = System.getLogger(DhtSocket.class.getName());

    private final DatagramSocket socket;

    public DhtSocket(DatagramSocket socket) {
        this.socket = requireNonNull(socket);
    }

    /**
     * Sends a {@link DhtMessage} to the specified address.
     *
     * @param message the message to send
     * @param address the address to send the message to
     * @throws IOException if an I/O error occurs
     */
    public void sendMessage(DhtMessage message, InetSocketAddress address) throws IOException {
        LOGGER.log(Level.DEBUG, "[DHT] Sending message: {0} to {1}", message, address);
        byte[] data = message.bencode();
        DatagramPacket packet = new DatagramPacket(data, data.length, address);
        socket.send(packet);
        LOGGER.log(Level.DEBUG, "[DHT] Sent message: {0} to {1}", message, address);
    }

    /**
     * Receives a {@link DhtMessage} from the socket, blocking until a message is received.
     * <p>
     * Response messages are decoded as {@link UndefinedResponse} since the information within response messages is
     * insufficient to determine the specific type of response.
     * <p>
     * It is up to the caller to determine the specific type of response by keeping track of outstanding
     * {@link Query queries}. The type of response can be determined by matching the {@link TransactionId} of the
     * response with the {@link TransactionId} of the {@link Query} that was sent and checking its {@link Method}.
     * Once the type of response is determined, subclasses of {@link DefinedResponse} can be used
     * to decode the {@link UndefinedResponse} as specific response types.
     *
     * @return the received message
     * @throws IOException if an I/O error occurs
     */
    public DhtMessage receiveMessage() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        BencodedMap bencodedMap = BencodedMap.decode(packet.getData());
        DhtMessage message = decode(bencodedMap);
        LOGGER.log(Level.DEBUG, "[DHT] Received {0}: {1}", message.getMessageType(), message);
        return message;
    }

    public static DhtMessage decode(BencodedMap bencodedMap) {
        MessageType messageType = MessageType.fromValue(bencodedMap.getChar(DhtMessage.KEY_MESSAGE_TYPE));
        switch (messageType) {
        case QUERY:
            return decodeQuery(bencodedMap);
        case RESPONSE:
            return UndefinedResponse.fromMap(bencodedMap);
        case ERROR:
            return Error.fromMap(bencodedMap);
        default:
            throw new AssertionError("Unknown message type: " + messageType);
        }
    }

    private static Query decodeQuery(BencodedMap map) {
        Method method = Method.fromValue(map.getString(Query.KEY_METHOD_NAME));
        switch (method) {
        case PING:
            return Ping.fromMap(map);
        case FIND_NODE:
            return FindNode.fromMap(map);
        case GET_PEERS:
            return GetPeers.fromMap(map);
        case ANNOUNCE_PEER:
            return AnnouncePeer.fromMap(map);
        default:
            throw new AssertionError("Unknown method: " + method);
        }
    }
}
