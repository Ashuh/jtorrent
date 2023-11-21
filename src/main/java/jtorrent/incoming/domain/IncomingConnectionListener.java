package jtorrent.incoming.domain;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import jtorrent.common.domain.Constants;
import jtorrent.common.domain.util.BackgroundTask;
import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.peer.domain.communication.PeerSocket;
import jtorrent.peer.domain.model.PeerContactInfo;
import jtorrent.peer.domain.model.message.Handshake;

public class IncomingConnectionListener {

    private static final Logger LOGGER = System.getLogger(IncomingConnectionListener.class.getName());

    private final ListenForIncomingConnectionsTask incomingConnectionsTask;
    private final LinkedBlockingQueue<IncomingConnection> incomingConnectionQueue = new LinkedBlockingQueue<>();

    public IncomingConnectionListener(ServerSocket serverSocket) {
        incomingConnectionsTask = new ListenForIncomingConnectionsTask(serverSocket);
    }

    public IncomingConnection waitForIncomingConnection() throws InterruptedException {
        return incomingConnectionQueue.take();
    }

    public void start() {
        incomingConnectionsTask.start();
    }

    public void stop() {
        incomingConnectionsTask.stop();
    }

    public static class IncomingConnection {

        private final PeerSocket peerSocket;
        private final Sha1Hash infoHash;

        public IncomingConnection(PeerSocket peerSocket, Sha1Hash infoHash) {
            this.peerSocket = requireNonNull(peerSocket);
            this.infoHash = requireNonNull(infoHash);
        }

        public PeerContactInfo getSource() {
            return peerSocket.getPeerContactInfo();
        }

        public Sha1Hash getInfoHash() {
            return infoHash;
        }

        public PeerSocket accept(boolean isDhtSupported) throws IOException {
            Handshake handshake = new Handshake(infoHash, Constants.PEER_ID.getBytes(), isDhtSupported);
            peerSocket.sendMessage(handshake);
            return peerSocket;
        }

        public void reject() throws IOException {
            peerSocket.close();
        }
    }

    private class ListenForIncomingConnectionsTask extends BackgroundTask {

        private static final int HANDSHAKE_TIMEOUT_MILLIS = 10000;

        private final ExecutorService executorService = Executors.newCachedThreadPool();
        private final ServerSocket serverSocket;

        public ListenForIncomingConnectionsTask(ServerSocket serverSocket) {
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
                    ListenForIncomingConnectionsTask.this.stop();
                }
            }
        }

        private void handleAcceptedSocket(Socket socket) {
            executorService.submit(() -> {
                PeerSocket peerSocket = new PeerSocket(socket);
                try {
                    Handshake handshake = waitForHandshake(peerSocket);
                    Sha1Hash infoHash = handshake.getInfoHash();
                    IncomingConnection incomingConnection = new IncomingConnection(peerSocket, infoHash);
                    incomingConnectionQueue.add(incomingConnection);
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
