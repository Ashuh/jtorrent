package jtorrent.peer.domain.model.message;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import jtorrent.peer.domain.model.message.typed.Bitfield;
import jtorrent.peer.domain.model.message.typed.Cancel;
import jtorrent.peer.domain.model.message.typed.Choke;
import jtorrent.peer.domain.model.message.typed.Have;
import jtorrent.peer.domain.model.message.typed.Interested;
import jtorrent.peer.domain.model.message.typed.MessageType;
import jtorrent.peer.domain.model.message.typed.NotInterested;
import jtorrent.peer.domain.model.message.typed.Piece;
import jtorrent.peer.domain.model.message.typed.Port;
import jtorrent.peer.domain.model.message.typed.Request;
import jtorrent.peer.domain.model.message.typed.TypedPeerMessage;
import jtorrent.peer.domain.model.message.typed.Unchoke;

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
