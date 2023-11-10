package jtorrent.incoming.domain;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jtorrent.common.domain.util.BackgroundTask;
import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.peer.domain.communication.PeerSocket;
import jtorrent.peer.domain.model.message.Handshake;

public class IncomingConnectionManager {

    private static final Logger LOGGER = System.getLogger(IncomingConnectionManager.class.getName());

    private final AcceptIncomingConnectionsTask incomingConnectionsTask;
    private final List<Listener> listeners = new ArrayList<>();

    public IncomingConnectionManager(ServerSocket serverSocket) {
        incomingConnectionsTask = new AcceptIncomingConnectionsTask(serverSocket);
    }

    public void start() {
        incomingConnectionsTask.start();
    }

    public void stop() {
        incomingConnectionsTask.stop();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }


    public interface Listener {

        void onIncomingPeerConnection(PeerSocket peerSocket, Sha1Hash infoHash);
    }

    private class AcceptIncomingConnectionsTask extends BackgroundTask {

        private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        private final ServerSocket serverSocket;

        public AcceptIncomingConnectionsTask(ServerSocket serverSocket) {
            this.serverSocket = requireNonNull(serverSocket);
        }

        @Override
        protected void execute() {
            try {
                Socket socket = serverSocket.accept();
                PeerSocket peerSocket = new PeerSocket(socket);
                LOGGER.log(Level.INFO, "Incoming connection from " + socket.getInetAddress());

                // TODO: use completablefuture
                final Future<?> handler = executorService.submit(new WaitForHandshakeTask(peerSocket));
                executorService.schedule(new TimeoutTask(handler), 10000, TimeUnit.MILLISECONDS);
            } catch (IOException e) {
                if (!isStopping()) {
                    LOGGER.log(Level.ERROR, "Error accepting incoming connection", e);
                    AcceptIncomingConnectionsTask.this.stop();
                }
            }
        }

        @Override
        public void doOnStop() {
            executorService.shutdownNow();
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class WaitForHandshakeTask implements Runnable {

        private final PeerSocket peerSocket;

        private WaitForHandshakeTask(PeerSocket peerSocket) {
            this.peerSocket = peerSocket;
        }

        @Override
        public void run() {
            try {
                Handshake handshake = peerSocket.waitForHandshake();
                listeners.forEach(listener -> listener.onIncomingPeerConnection(peerSocket, handshake.getInfoHash()));
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "{0} disconnected", peerSocket.getRemoteAddress());
            }
        }
    }

    private static class TimeoutTask implements Runnable {

        private final Future<?> future;

        private TimeoutTask(Future<?> future) {
            this.future = future;
        }

        @Override
        public void run() {
            future.cancel(true);
        }
    }
}
