package jtorrent.domain.socket;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jtorrent.domain.model.dht.message.DhtMessage;
import jtorrent.domain.model.dht.message.DhtMessageDecoder;
import jtorrent.domain.model.dht.message.TransactionId;
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

    public CompletableFuture<PingResponse> sendPing(Ping query, InetSocketAddress address) {
        return sendQuery(query, address);
    }

    private <T extends Query, R extends DefinedResponse> CompletableFuture<R> sendQuery(T query,
            InetSocketAddress address) {
        CompletableFuture<R> completableFuture = completableFutureWithTimeout(TIMEOUT_SECS);
        txIdToMethod.put(query.getTransactionId(), query.getMethod());
        txIdToFuture.put(query.getTransactionId(), completableFuture);

        completableFuture.exceptionally(throwable -> {
            if (throwable instanceof TimeoutException) {
                LOGGER.log(Level.ERROR, "[DHT] Timeout while waiting for response to query {0}", query);
            } else {
                LOGGER.log(Level.ERROR, "[DHT] Failed to send query {0}: {1}", query, throwable.getMessage());
            }
            txIdToMethod.remove(query.getTransactionId());
            txIdToFuture.remove(query.getTransactionId());
            return null;
        });

        try {
            sendMessage(query, address);
        } catch (IOException e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
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

    public CompletableFuture<FindNodeResponse> sendFindNode(FindNode query, InetSocketAddress address) {
        return sendQuery(query, address);
    }

    public CompletableFuture<AnnouncePeerResponse> sendAnnouncePeer(AnnouncePeer query, InetSocketAddress address) {
        return sendQuery(query, address);
    }

    public CompletableFuture<GetPeersResponse> sendGetPeers(GetPeers query, InetSocketAddress address) {
        return sendQuery(query, address);
    }


    private Method getTransactionId(TransactionId transactionId) {
        Method method = txIdToMethod.get(transactionId);
        if (method == null) {
            throw new IllegalArgumentException("No method found for transaction id: " + transactionId);
        }
        return method;
    }

    public interface QueryHandler {

        void handle(Ping ping);

        void handle(FindNode findNode);

        void handle(AnnouncePeer announcePeer);

        void handle(GetPeers getPeers);
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
                DhtMessage message = receiveMessage();
                handleMessage(message);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "[DHT] Error while receiving message: {0}", e.getMessage());
                HandleIncomingMessagesTask.this.stop();
            } catch (Exception e) {
                // TODO: use more specific exception
                LOGGER.log(Level.ERROR, "[DHT] Error while handling message: {0}", e.getMessage());
            }
        }

        /**
         * Receives a {@link DhtMessage} from the socket, blocking until a message is received.
         *
         * @return the received message
         * @throws IOException if an I/O error occurs
         */
        private DhtMessage receiveMessage() throws IOException {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            DhtMessage message = dhtMessageDecoder.decode(packet.getData());
            LOGGER.log(Level.DEBUG, "[DHT] Received {0}: {1}", message.getMessageType(), message);
            return message;
        }

        private void handleMessage(DhtMessage message) {
            switch (message.getMessageType()) {
            case QUERY:
                handleQuery((Query) message);
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

        private void handleQuery(Query query) {
            LOGGER.log(Level.DEBUG, "[DHT] Received {0} query: {1}", query.getMethod(), query);

            switch (query.getMethod()) {
            case PING:
                queryHandler.handle((Ping) query);
                break;
            case FIND_NODE:
                queryHandler.handle((FindNode) query);
                break;
            case ANNOUNCE_PEER:
                queryHandler.handle((AnnouncePeer) query);
                break;
            case GET_PEERS:
                queryHandler.handle((GetPeers) query);
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
            CompletableFuture<T> completableFuture = (CompletableFuture<T>) txIdToFuture.remove(transactionId);

            if (completableFuture == null) {
                logNoOutstandingQueryFound(transactionId);
                return;
            }

            completableFuture.complete(response);
        }

        private void handleError(Error error) {
            LOGGER.log(Level.DEBUG, "Received error: {0}", error);

            TransactionId transactionId = error.getTransactionId();
            txIdToMethod.remove(transactionId);
            CompletableFuture<? extends DefinedResponse> future = txIdToFuture.remove(transactionId);

            if (future == null) {
                logNoOutstandingQueryFound(transactionId);
                return;
            }

            future.completeExceptionally(new RuntimeException(error.getErrorMessage()));
        }

        private void logNoOutstandingQueryFound(TransactionId transactionId) {
            LOGGER.log(Level.ERROR, "[DHT] No outstanding query found for transaction id: {0}", transactionId);
        }
    }
}
