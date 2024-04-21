package jtorrent.domain.inbound;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jtorrent.domain.common.util.BackgroundTask;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.common.util.logging.MdcUtil;
import jtorrent.domain.peer.communication.PeerSocket;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.peer.model.message.Handshake;

public class InboundConnectionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundConnectionListener.class);

    private final ListenForInboundConnectionsTask listenForInboundConnectionsTask;
    private final LinkedBlockingQueue<InboundConnection> inboundConnections = new LinkedBlockingQueue<>();

    public InboundConnectionListener(ServerSocket serverSocket) {
        listenForInboundConnectionsTask = new ListenForInboundConnectionsTask(serverSocket);
    }

    public InboundConnection waitForIncomingConnection() throws InterruptedException {
        return inboundConnections.take();
    }

    public void start() {
        LOGGER.debug(Markers.INBOUND, "Starting inbound connection listener");
        listenForInboundConnectionsTask.start();
        LOGGER.info(Markers.INBOUND, "Inbound connection listener started");
    }

    public void stop() {
        LOGGER.debug(Markers.INBOUND, "Stopping inbound connection listener");
        listenForInboundConnectionsTask.stop();
        LOGGER.info(Markers.INBOUND, "Inbound connection listener stopped");
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
                MdcUtil.putPeer(socket.getInetAddress(), socket.getPort());
                LOGGER.info(Markers.INBOUND, "Inbound connection");
                handleAcceptedSocket(socket);
            } catch (IOException e) {
                if (!isStopping()) {
                    LOGGER.error(Markers.INBOUND, "Failed to accept inbound socket connection", e);
                    ListenForInboundConnectionsTask.this.stop();
                }
            } finally {
                MdcUtil.removePeer();
            }
        }

        private void handleAcceptedSocket(Socket socket) {
            Map<String, String> context = MDC.getCopyOfContextMap();
            executorService.submit(() -> {
                MDC.setContextMap(context);
                PeerSocket peerSocket = new PeerSocket(socket);
                try {
                    Handshake handshake = waitForHandshake(peerSocket);
                    Sha1Hash infoHash = handshake.getInfoHash();
                    InboundConnection inboundConnection = new InboundConnection(peerSocket, infoHash);
                    inboundConnections.add(inboundConnection);
                } catch (IOException e) {
                    if (e instanceof SocketTimeoutException) {
                        LOGGER.error(Markers.INBOUND, "Failed to receive handshake: Time out");
                    } else {
                        LOGGER.error(Markers.INBOUND, "Failed to receive handshake", e);
                    }
                    tryCloseSocket(peerSocket);
                } finally {
                    MDC.clear();
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
                LOGGER.error(Markers.INBOUND, "Failed to close socket", e);
            }
        }

        @Override
        public void doOnStop() {
            executorService.shutdownNow();
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.error(Markers.INBOUND, "Failed to close server socket", e);
            }
        }
    }
}
