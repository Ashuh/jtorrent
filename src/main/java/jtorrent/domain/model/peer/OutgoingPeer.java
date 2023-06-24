package jtorrent.domain.model.peer;

import static jtorrent.domain.Constants.PEER_ID;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.Socket;

import jtorrent.domain.model.peer.exception.InfoHashMismatchException;
import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.util.Sha1Hash;

public class OutgoingPeer extends Peer {

    private static final System.Logger LOGGER = System.getLogger(OutgoingPeer.class.getName());

    public OutgoingPeer(InetAddress address, int port) {
        super(address, port);
    }

    @Override
    public void connect(Sha1Hash infoHash) throws IOException {
        if (infoHash == null) {
            throw new IllegalArgumentException("infoHash cannot be null");
        }

        LOGGER.log(Level.DEBUG, "Connecting to peer: {0}:{1}", address, port);
        this.socket = new Socket(address, port);

        Handshake handshake = new Handshake(infoHash, PEER_ID.getBytes());
        sendMessage(handshake);

        Handshake inHandshake = receiveHandshake();
        if (!inHandshake.getInfoHash().equals(infoHash)) {
            throw new InfoHashMismatchException(infoHash, inHandshake.getInfoHash());
        }

        LOGGER.log(System.Logger.Level.DEBUG, "Sent handshake to peer: {0}:{1}", address, port);
    }
}
