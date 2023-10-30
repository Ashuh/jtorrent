package jtorrent.domain.socket;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jtorrent.domain.model.dht.message.DhtMessage;
import jtorrent.domain.model.dht.message.TransactionId;
import jtorrent.domain.model.dht.message.decoder.DhtDecodingException;
import jtorrent.domain.model.dht.message.decoder.DhtMessageDecoder;
import jtorrent.domain.model.dht.message.error.Error;
import jtorrent.domain.model.dht.message.query.AnnouncePeer;
import jtorrent.domain.model.dht.message.query.FindNode;
import jtorrent.domain.model.dht.message.query.GetPeers;
import jtorrent.domain.model.dht.message.query.Method;
import jtorrent.domain.model.dht.message.query.Ping;
import jtorrent.domain.model.dht.message.query.Query;
import jtorrent.domain.model.dht.message.response.AnnouncePeerResponse;
import jtorrent.domain.model.dht.message.response.DefinedResponse;
import jtorrent.domain.model.dht.message.response.FindNodeResponse;
import jtorrent.domain.model.dht.message.response.GetPeersResponse;
import jtorrent.domain.model.dht.message.response.PingResponse;
import jtorrent.domain.model.dht.node.NodeContactInfo;
import jtorrent.domain.util.BackgroundTask;

public class DhtSocket {

    private static final Logger LOGGER = System.getLogger(DhtSocket.class.getName());
    private static final int TIMEOUT_SECS = 3;

    private final DatagramSocket socket;
    private final HandleIncomingMessagesTask handleIncomingMessagesTask;
    private final Map<TransactionId, Method> txIdToMethod = new ConcurrentHashMap<>();
    private final Map<TransactionId, CompletableFuture<? extends DefinedResponse>> txIdToFuture =
            new ConcurrentHashMap<>();
    private final DhtMessageDecoder dhtMessageDecoder = new DhtMessageDecoder(this::getTransactionId);

    public DhtSocket(DatagramSocket socket, QueryHandler queryHandler) {
        this.socket = requireNonNull(socket);
        this.handleIncomingMessagesTask = new HandleIncomingMessagesTask(queryHandler);
    }

    /**
     * Starts receiving messages from the socket. Without calling this method, no messages will be received.
     */
    public void start() {
        handleIncomingMessagesTask.start();
    }

    /**
     * Stops receiving messages from the socket. After calling this method, no messages will be received.
     * Once stopped, the socket cannot be started again.
     */
    public void stop() {
        handleIncomingMessagesTask.stop();
    }

    public CompletableFuture<PingResponse> sendPing(Ping ping, InetSocketAddress address) {
        return sendQuery(ping, address);
    }

    public CompletableFuture<FindNodeResponse> sendFindNode(FindNode findNode, InetSocketAddress address) {
        return sendQuery(findNode, address);
    }

    public CompletableFuture<GetPeersResponse> sendGetPeers(GetPeers getPeers, InetSocketAddress address) {
        return sendQuery(getPeers, address);
    }

    public CompletableFuture<AnnouncePeerResponse> sendAnnouncePeer(AnnouncePeer announcePeer,
            InetSocketAddress address) {
        return sendQuery(announcePeer, address);
    }

    private <T extends Query, R extends DefinedResponse> CompletableFuture<R> sendQuery(T query,
            InetSocketAddress address) {
        CompletableFuture<R> completableFuture = completableFutureWithTimeout(TIMEOUT_SECS);
        txIdToMethod.put(query.getTransactionId(), query.getMethod());
        txIdToFuture.put(query.getTransactionId(), completableFuture);

        completableFuture.whenComplete((response, throwable) -> {
            txIdToMethod.remove(query.getTransactionId());
            txIdToFuture.remove(query.getTransactionId());
        });

        try {
            sendMessage(query, address);
        } catch (IOException e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    public void sendResponse(DefinedResponse response, InetSocketAddress address) throws IOException {
        sendMessage(response, address);
    }

    private static <T> CompletableFuture<T> completableFutureWithTimeout(int timeoutSecs) {
        return new CompletableFuture<T>().orTimeout(timeoutSecs, TimeUnit.SECONDS);
    }

    /**
     * Sends a {@link DhtMessage} to the specified address.
     *
     * @param message the message to send
     * @param address the address to send the message to
     * @throws IOException if an I/O error occurs
     */
    private void sendMessage(DhtMessage message, InetSocketAddress address) throws IOException {
        LOGGER.log(Level.DEBUG, "[DHT] Sending message: {0} to {1}", message, address);
        byte[] data = message.bencode();
        DatagramPacket packet = new DatagramPacket(data, data.length, address);
        socket.send(packet);
        LOGGER.log(Level.DEBUG, "[DHT] Sent message: {0} to {1}", message, address);
    }

    private Optional<Method> getTransactionId(TransactionId transactionId) {
        return Optional.ofNullable(txIdToMethod.get(transactionId));
    }

    public interface QueryHandler {

        void handle(Ping ping, NodeContactInfo nodeContactInfo);

        void handle(FindNode findNode, NodeContactInfo nodeContactInfo);

        void handle(GetPeers getPeers, NodeContactInfo nodeContactInfo);

        void handle(AnnouncePeer announcePeer, NodeContactInfo nodeContactInfo);
    }

    private class HandleIncomingMessagesTask extends BackgroundTask {

        private static final String FORMAT_UNKNOWN_METHOD = "Unknown method: %s";

        private final QueryHandler queryHandler;

        private HandleIncomingMessagesTask(QueryHandler queryHandler) {
            this.queryHandler = requireNonNull(queryHandler);
        }

        @Override
        protected void execute() {
            try {
                LOGGER.log(Level.DEBUG, "[DHT] Waiting for message");
                IncomingMessage incomingMessage = receiveMessage();
                handleMessage(incomingMessage.getMessage(), incomingMessage.getAddress());
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[DHT] Error receiving message: " + e.getMessage(), e);
                HandleIncomingMessagesTask.this.stop();
            } catch (DhtDecodingException e) {
                LOGGER.log(Level.ERROR, "[DHT] Error decoding message: " + e.getMessage(), e);
            }
        }

        /**
         * Receives a {@link DhtMessage} from the socket, blocking until a message is received.
         *
         * @return the received message
         * @throws IOException if an I/O error occurs
         */
        private IncomingMessage receiveMessage() throws IOException, DhtDecodingException {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());
            DhtMessage message = dhtMessageDecoder.decode(packet.getData());
            return new IncomingMessage(address, message);
        }

        private void handleMessage(DhtMessage message, InetSocketAddress address) {
            switch (message.getMessageType()) {
            case QUERY:
                handleQuery((Query) message, address);
                break;
            case RESPONSE:
                handleResponse((DefinedResponse) message);
                break;
            case ERROR:
                handleError((Error) message);
                break;
            default:
                throw new AssertionError("Unknown message type: " + message.getMessageType());
            }
        }

        private void handleQuery(Query query, InetSocketAddress address) {
            LOGGER.log(Level.DEBUG, "[DHT] Received {0} query: {1}", query.getMethod(), query);
            NodeContactInfo nodeContactInfo = new NodeContactInfo(query.getId(), address);
            switch (query.getMethod()) {
            case PING:
                queryHandler.handle((Ping) query, nodeContactInfo);
                break;
            case FIND_NODE:
                queryHandler.handle((FindNode) query, nodeContactInfo);
                break;
            case ANNOUNCE_PEER:
                queryHandler.handle((AnnouncePeer) query, nodeContactInfo);
                break;
            case GET_PEERS:
                queryHandler.handle((GetPeers) query, nodeContactInfo);
                break;
            default:
                throw new AssertionError(String.format(FORMAT_UNKNOWN_METHOD, query.getMethod()));
            }
        }

        @SuppressWarnings("unchecked")
        private <T extends DefinedResponse> void handleResponse(T response) {
            LOGGER.log(Level.DEBUG, "[DHT] Received {0} response: {1}", response.getMethod(), response);

            TransactionId transactionId = response.getTransactionId();
            txIdToMethod.remove(transactionId);
            CompletableFuture<T> completableFuture = (CompletableFuture<T>) txIdToFuture.get(transactionId);

            if (completableFuture == null) {
                logNoOutstandingQueryFound(transactionId);
                return;
            }

            completableFuture.complete(response);
        }

        private void handleError(Error error) {
            LOGGER.log(Level.DEBUG, "[DHT] Received error: {0}", error);

            TransactionId transactionId = error.getTransactionId();
            txIdToMethod.remove(transactionId);
            CompletableFuture<? extends DefinedResponse> future = txIdToFuture.get(transactionId);

            if (future == null) {
                logNoOutstandingQueryFound(transactionId);
                return;
            }

            future.completeExceptionally(new DhtErrorException(error));
        }

        private void logNoOutstandingQueryFound(TransactionId transactionId) {
            LOGGER.log(Level.ERROR, "[DHT] No outstanding query found for transaction id: {0}", transactionId);
        }
    }

    private static class IncomingMessage {

        private final InetSocketAddress address;
        private final DhtMessage message;

        public IncomingMessage(InetSocketAddress address, DhtMessage message) {
            this.address = requireNonNull(address);
            this.message = requireNonNull(message);
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public DhtMessage getMessage() {
            return message;
        }
    }
}
