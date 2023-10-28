package jtorrent.domain.model.peer;

import static jtorrent.domain.Constants.PEER_ID;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.Socket;

import jtorrent.domain.model.peer.exception.InfoHashMismatchException;
import jtorrent.domain.model.peer.message.Handshake;
import jtorrent.domain.util.Sha1Hash;

public class OutgoingPeer extends Peer {

    private static final Logger LOGGER = System.getLogger(OutgoingPeer.class.getName());

    public OutgoingPeer(PeerContactInfo peerContactInfo) {
        super(peerContactInfo);
    }

    @Override
    public void connect(Sha1Hash infoHash, boolean isDhtSupported) throws IOException {
        if (infoHash == null) {
            throw new IllegalArgumentException("infoHash cannot be null");
        }

        LOGGER.log(Level.DEBUG, "Connecting to peer: {0}", peerContactInfo);
        this.socket = new Socket(peerContactInfo.getAddress(), peerContactInfo.getPort());

        Handshake handshake = new Handshake(infoHash, PEER_ID.getBytes(), isDhtSupported);
        sendMessage(handshake);

        Handshake inHandshake = receiveHandshake();
        if (!inHandshake.getInfoHash().equals(infoHash)) {
            throw new InfoHashMismatchException(infoHash, inHandshake.getInfoHash());
        }

        LOGGER.log(Level.DEBUG, "Sent handshake to peer: {0}", peerContactInfo);
    }
}
