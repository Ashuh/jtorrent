package jtorrent.domain.model.peer;

import static java.util.Objects.requireNonNull;

import java.net.InetAddress;
import java.util.Objects;

public class Peer {

    private final InetAddress address;
    private final int port; // unsigned short

    public Peer(InetAddress address, int port) {
        this.address = requireNonNull(address);
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Peer peer = (Peer) o;
        return port == peer.port && address.equals(peer.address);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "address=" + address +
                ", port=" + port +
                '}';
    }
}
