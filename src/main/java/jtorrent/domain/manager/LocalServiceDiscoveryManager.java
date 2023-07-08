package jtorrent.domain.manager;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jtorrent.domain.Constants;
import jtorrent.domain.model.localservicediscovery.Announce;
import jtorrent.domain.util.Sha1Hash;

public class LocalServiceDiscoveryManager {

    private static final Logger LOGGER = System.getLogger(LocalServiceDiscoveryManager.class.getName());
    private static final String COOKIE = UUID.randomUUID().toString();
    private static final InetAddress MULTICAST_GROUP;

    static {
        try {
            MULTICAST_GROUP = InetAddress.getByName("239.192.152.143");
        } catch (UnknownHostException e) {
            throw new AssertionError("This should never happen", e);
        }
    }

    private static final int PORT = 6771;
    private static final int TIME_TO_LIVE = 3;
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(2);

    private final MulticastSocket socket;
    private final List<Listener> listeners = new ArrayList<>();
    private final LinkedBlockingDeque<Sha1Hash> infoHashQueue = new LinkedBlockingDeque<>();
    private final Thread thread;

    public LocalServiceDiscoveryManager() throws IOException {
        socket = new MulticastSocket(PORT);
        socket.setTimeToLive(TIME_TO_LIVE);
        socket.joinGroup(MULTICAST_GROUP);

        thread = new Thread(new ReceiveTask());
        thread.start();

        EXECUTOR_SERVICE.execute(new AnnounceTask());
    }

    public void addInfoHash(Sha1Hash infoHash) {
        infoHashQueue.add(infoHash);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public interface Listener {

        void onAnnounceReceived(Announce announce, InetAddress sourceAddress);
    }

    private class AnnounceTask implements Runnable {

        @Override
        public void run() {
            try {
                Sha1Hash infoHash = infoHashQueue.take();
                announce(infoHash);
            } catch (InterruptedException e) {
                LOGGER.log(Level.DEBUG, "Announce task interrupted", e);
                EXECUTOR_SERVICE.execute(new AnnounceTask());
                Thread.currentThread().interrupt();
            }
        }

        private void announce(Sha1Hash infoHash) {
            String host = MULTICAST_GROUP.getHostAddress() + ":" + PORT;
            Announce announce = new Announce(host, Constants.PORT, Set.of(infoHash), COOKIE);
            try {
                sendAnnounce(announce);
                EXECUTOR_SERVICE.schedule(new AnnounceTask(), 1, TimeUnit.MINUTES);
                EXECUTOR_SERVICE.schedule(() -> infoHashQueue.add(infoHash), 5, TimeUnit.MINUTES);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Failed to send Local Service Discovery announce", e);
                EXECUTOR_SERVICE.execute(new AnnounceTask());
                EXECUTOR_SERVICE.execute(() -> infoHashQueue.add(infoHash));
            }
        }

        private void sendAnnounce(Announce announce) throws IOException {
            LOGGER.log(Level.DEBUG, "Sending Local Service Discovery announce: " + announce);
            byte[] announceBytes = announce.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(announceBytes, announceBytes.length, MULTICAST_GROUP, PORT);
            socket.send(packet);
        }
    }

    private class ReceiveTask implements Runnable {

        private boolean isRunning = true;

        @Override
        public void run() {
            while (isRunning) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                    Announce announce = Announce.fromString(receivedMessage);

                    LOGGER.log(Level.DEBUG, "Received Local Service Discovery announce: "
                            + announce
                            + "\n"
                            + "from "
                            + packet.getAddress()
                            + ":"
                            + packet.getPort());

                    boolean isOwnAnnounce = announce.getCookie()
                            .map(cookie -> cookie.equals(COOKIE))
                            .orElse(false);

                    if (isOwnAnnounce) {
                        LOGGER.log(Level.DEBUG, "Ignoring own announce");
                        continue;
                    }

                    InetAddress address = packet.getAddress();
                    listeners.forEach(listener -> listener.onAnnounceReceived(announce, address));
                } catch (IOException e) {
                    LOGGER.log(Level.ERROR, "Error while receiving Local Service Discovery announce", e);
                }
            }
        }
    }
}
