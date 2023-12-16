package jtorrent.peer.domain.model.message;

public interface PeerMessage {

    byte[] pack();

    /**
     * Gets the size of the message in bytes.
     */
    int getMessageSize();
}
