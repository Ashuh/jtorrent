package jtorrent.domain.manager;

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
import jtorrent.domain.util.Sha1Hash;

public class IncomingConnectionManager implements Runnable {

    private static final Logger LOGGER = System.getLogger(IncomingConnectionManager.class.getName());
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2);

    private final ServerSocket serverSocket;
    private final List<Listener> listeners = new ArrayList<>();
    private final Thread thread;
    private boolean isRunning = true;

    public IncomingConnectionManager(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void run() {
        LOGGER.log(Level.INFO, "Listening for incoming connections");
        while (isRunning) {
            try {
                Socket socket = serverSocket.accept();
                LOGGER.log(Level.INFO, "Incoming connection from " + socket.getInetAddress());
                Peer peer = new IncomingPeer(socket);
                final Future<?> handler = EXECUTOR.submit(new WaitForHandshakeTask(peer));
                EXECUTOR.schedule(new TimeoutTask(handler), 10000, TimeUnit.MILLISECONDS);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Error accepting connection", e);
            }
        }
    }

    public interface Listener {

        void onIncomingPeerConnection(Peer peer, Sha1Hash infoHash);
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
                LOGGER.log(Level.ERROR, "{0} disconnected", peer.getAddress());
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
