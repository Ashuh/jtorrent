package jtorrent.domain.model.peer.message;

import static jtorrent.domain.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import jtorrent.domain.model.peer.message.typed.Bitfield;
import jtorrent.domain.model.peer.message.typed.Cancel;
import jtorrent.domain.model.peer.message.typed.Choke;
import jtorrent.domain.model.peer.message.typed.Have;
import jtorrent.domain.model.peer.message.typed.Interested;
import jtorrent.domain.model.peer.message.typed.MessageType;
import jtorrent.domain.model.peer.message.typed.NotInterested;
import jtorrent.domain.model.peer.message.typed.Piece;
import jtorrent.domain.model.peer.message.typed.Port;
import jtorrent.domain.model.peer.message.typed.Request;
import jtorrent.domain.model.peer.message.typed.TypedPeerMessage;
import jtorrent.domain.model.peer.message.typed.Unchoke;

public class PeerMessageUnpacker {

    private static final Logger LOGGER = System.getLogger(PeerMessageUnpacker.class.getName());

    /**
     * Unpacks a message from a byte array.
     *
     * @param messageBytes the bytes to unpack.
     *                     This should contain the bytes of the complete message excluding the length prefix.
     *                     Cannot be null.
     * @return the unpacked message
     */
    public PeerMessage unpack(byte[] messageBytes) {
        requireNonNull(messageBytes);

        if (messageBytes.length == 0) {
            return new KeepAlive();
        }

        return unpackTypedMessage(messageBytes);
    }

    private PeerMessage unpackTypedMessage(byte[] messageBytes) {
        MessageType messageType = MessageType.fromValue(messageBytes[0]);
        byte[] payload = new byte[messageBytes.length - 1];
        System.arraycopy(messageBytes, 1, payload, 0, payload.length);
        return unpackTypedMessage(messageType, payload);
    }

    private TypedPeerMessage unpackTypedMessage(MessageType messageType, byte[] payload) {
        LOGGER.log(Level.DEBUG, "Unpacking {0} message", messageType);

        switch (messageType) {
        case CHOKE:
            return new Choke();
        case UNCHOKE:
            return new Unchoke();
        case INTERESTED:
            return new Interested();
        case NOT_INTERESTED:
            return new NotInterested();
        case HAVE:
            return Have.unpack(payload);
        case BITFIELD:
            return Bitfield.unpack(payload);
        case REQUEST:
            return Request.unpack(payload);
        case PIECE:
            return Piece.unpack(payload);
        case CANCEL:
            return Cancel.unpack(payload);
        case PORT:
            return Port.unpack(payload);
        default:
            throw new AssertionError("Unknown message type: " + messageType);
        }
    }
}
