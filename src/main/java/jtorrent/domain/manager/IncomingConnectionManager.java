package jtorrent.domain.manager;

import static java.util.Objects.requireNonNull;

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

import jtorrent.domain.model.peer.IncomingPeer;
import jtorrent.domain.model.peer.Peer;
import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.util.BackgroundTask;
import jtorrent.domain.util.Sha1Hash;

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

        void onIncomingPeerConnection(Peer peer, Sha1Hash infoHash);
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
                LOGGER.log(Level.INFO, "Incoming connection from " + socket.getInetAddress());
                Peer peer = new IncomingPeer(socket);
                final Future<?> handler = executorService.submit(new WaitForHandshakeTask(peer));
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

        private final Peer peer;

        private WaitForHandshakeTask(Peer peer) {
            this.peer = peer;
        }

        @Override
        public void run() {
            try {
                Handshake handshake = peer.receiveHandshake();
                listeners.forEach(listener -> listener.onIncomingPeerConnection(peer, handshake.getInfoHash()));
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "{0} disconnected", peer.getPeerContactInfo());
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
