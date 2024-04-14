package jtorrent.domain.lsd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jtorrent.domain.common.Constants;
import jtorrent.domain.common.util.BackgroundTask;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.lsd.model.Announce;

public class LocalServiceDiscoveryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalServiceDiscoveryManager.class);
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

    private final MulticastSocket socket;
    private final List<Listener> listeners = new ArrayList<>();
    private final LinkedBlockingQueue<Sha1Hash> infoHashQueue = new LinkedBlockingQueue<>();
    private final ListenForAnnouncementsTask listenForAnnouncementsTask = new ListenForAnnouncementsTask();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    public LocalServiceDiscoveryManager() throws IOException {
        socket = new MulticastSocket(PORT);
        socket.setTimeToLive(TIME_TO_LIVE);
        socket.joinGroup(MULTICAST_GROUP);
    }

    public void start() {
        LOGGER.info(Markers.LSD, "Starting Local Service Discovery");
        listenForAnnouncementsTask.start();
        executorService.execute(new AnnounceTask());
    }

    public void stop() {
        LOGGER.info(Markers.LSD, "Stopping Local Service Discovery");
        listenForAnnouncementsTask.stop();
        executorService.shutdownNow();
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
                LOGGER.debug(Markers.LSD, "Announce task interrupted", e);
                executorService.execute(new AnnounceTask());
                Thread.currentThread().interrupt();
            }
        }

        private void announce(Sha1Hash infoHash) {
            String host = MULTICAST_GROUP.getHostAddress() + ":" + PORT;
            Announce announce = new Announce(host, Constants.PORT, Set.of(infoHash), COOKIE);
            try {
                sendAnnounce(announce);
                executorService.schedule(new AnnounceTask(), 1, TimeUnit.MINUTES);
                executorService.schedule(() -> infoHashQueue.add(infoHash), 5, TimeUnit.MINUTES);
            } catch (IOException e) {
                LOGGER.error(Markers.LSD, "Failed to send announce", e);
                executorService.execute(new AnnounceTask());
                executorService.execute(() -> infoHashQueue.add(infoHash));
            }
        }

        private void sendAnnounce(Announce announce) throws IOException {
            byte[] announceBytes = announce.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(announceBytes, announceBytes.length, MULTICAST_GROUP, PORT);
            socket.send(packet);
            LOGGER.info(Markers.LSD, "Sent announce\n {}", announce);
        }
    }

    private class ListenForAnnouncementsTask extends BackgroundTask {

        private final DatagramPacket packet;

        public ListenForAnnouncementsTask() {
            byte[] buffer = new byte[1024];
            packet = new DatagramPacket(buffer, buffer.length);
        }

        @Override
        protected void execute() {
            try {
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                Announce announce = Announce.fromString(receivedMessage);

                boolean isOwnAnnounce = announce.getCookie()
                        .map(cookie -> cookie.equals(COOKIE))
                        .orElse(false);

                if (isOwnAnnounce) {
                    LOGGER.debug(Markers.LSD, "Ignoring own announce");
                    return;
                }

                LOGGER.info(Markers.LSD, "Received announce from {}:{}\n {}",
                        packet.getAddress(), packet.getPort(), announce);

                InetAddress address = packet.getAddress();
                listeners.forEach(listener -> listener.onAnnounceReceived(announce, address));
            } catch (IOException e) {
                if (!isStopping()) {
                    LOGGER.error(Markers.LSD, "Failed to receive announce", e);
                    ListenForAnnouncementsTask.this.stop();
                }
            }
        }

        @Override
        protected void doOnStop() {
            socket.close();
        }
    }
}
