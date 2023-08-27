package jtorrent.domain.model.peer;

import static jtorrent.domain.Constants.PEER_ID;

import java.io.IOException;
import java.net.Socket;

import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.util.Sha1Hash;

public class IncomingPeer extends Peer {

    private static final System.Logger LOGGER = System.getLogger(IncomingPeer.class.getName());

    public IncomingPeer(Socket socket) {
        super(socket);
    }

    @Override
    public void connect(Sha1Hash infoHash, boolean isDhtSupported) throws IOException {
        if (infoHash == null) {
            throw new IllegalArgumentException("infoHash cannot be null");
        }
        LOGGER.log(System.Logger.Level.DEBUG, "Connecting to peer: {0}:{1}", address, port);
        Handshake handshake = new Handshake(infoHash, PEER_ID.getBytes(), isDhtSupported);
        sendMessage(handshake);
        LOGGER.log(System.Logger.Level.DEBUG, "Sent handshake to peer: {0}:{1}", address, port);
    }
}
