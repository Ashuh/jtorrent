package jtorrent.domain.inbound;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import jtorrent.domain.common.util.BackgroundTask;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.peer.communication.PeerSocket;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.peer.model.message.Handshake;

public class InboundConnectionListener {

    private static final Logger LOGGER = System.getLogger(InboundConnectionListener.class.getName());

    private final ListenForInboundConnectionsTask listenForInboundConnectionsTask;
    private final LinkedBlockingQueue<InboundConnection> inboundConnections = new LinkedBlockingQueue<>();

    public InboundConnectionListener(ServerSocket serverSocket) {
        listenForInboundConnectionsTask = new ListenForInboundConnectionsTask(serverSocket);
    }

    public InboundConnection waitForIncomingConnection() throws InterruptedException {
        return inboundConnections.take();
    }

    public void start() {
        listenForInboundConnectionsTask.start();
    }

    public void stop() {
        listenForInboundConnectionsTask.stop();
    }

    public static class InboundConnection {

        private final PeerSocket peerSocket;
        private final Sha1Hash infoHash;

        public InboundConnection(PeerSocket peerSocket, Sha1Hash infoHash) {
            this.peerSocket = requireNonNull(peerSocket);
            this.infoHash = requireNonNull(infoHash);
        }

        public PeerContactInfo getSource() {
            return peerSocket.getPeerContactInfo();
        }

        public Sha1Hash getInfoHash() {
            return infoHash;
        }

        public PeerSocket accept() {
            return peerSocket;
        }

        public void reject() throws IOException {
            peerSocket.close();
        }
    }

    private class ListenForInboundConnectionsTask extends BackgroundTask {

        private static final int HANDSHAKE_TIMEOUT_MILLIS = 10000;

        private final ExecutorService executorService = Executors.newCachedThreadPool();
        private final ServerSocket serverSocket;

        public ListenForInboundConnectionsTask(ServerSocket serverSocket) {
            this.serverSocket = requireNonNull(serverSocket);
        }

        @Override
        protected void execute() {
            try {
                Socket socket = serverSocket.accept();
                LOGGER.log(Level.INFO, "Incoming connection from " + socket.getRemoteSocketAddress());
                handleAcceptedSocket(socket);
            } catch (IOException e) {
                if (!isStopping()) {
                    LOGGER.log(Level.ERROR, "Error accepting incoming connection", e);
                    ListenForInboundConnectionsTask.this.stop();
                }
            }
        }

        private void handleAcceptedSocket(Socket socket) {
            executorService.submit(() -> {
                PeerSocket peerSocket = new PeerSocket(socket);
                try {
                    Handshake handshake = waitForHandshake(peerSocket);
                    Sha1Hash infoHash = handshake.getInfoHash();
                    InboundConnection inboundConnection = new InboundConnection(peerSocket, infoHash);
                    inboundConnections.add(inboundConnection);
                } catch (IOException e) {
                    if (e instanceof SocketTimeoutException) {
                        LOGGER.log(Level.ERROR,
                                String.format("[%s] Handshake timed out", socket.getRemoteSocketAddress()), e);
                    } else {
                        LOGGER.log(Level.ERROR, String.format("[%s] Error accepting incoming connection",
                                socket.getRemoteSocketAddress()), e);
                    }
                    tryCloseSocket(peerSocket);
                }
            });
        }

        private Handshake waitForHandshake(PeerSocket peerSocket) throws IOException {
            int originalTimeout = peerSocket.getTimeout();
            peerSocket.setTimeout(HANDSHAKE_TIMEOUT_MILLIS);
            Handshake handshake = peerSocket.waitForHandshake();
            peerSocket.setTimeout(originalTimeout);
            return handshake;
        }

        private void tryCloseSocket(PeerSocket peerSocket) {
            try {
                peerSocket.close();
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Error closing socket", e);
            }
        }

        @Override
        public void doOnStop() {
            executorService.shutdownNow();
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Error closing server socket", e);
            }
        }
    }
}
