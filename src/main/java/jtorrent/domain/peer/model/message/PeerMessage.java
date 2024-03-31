package jtorrent.domain.peer.model.message;

public interface PeerMessage {

    byte[] pack();

    /**
     * Gets the size of the message in bytes.
     */
    int getMessageSize();
}
