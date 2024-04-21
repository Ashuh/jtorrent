package jtorrent.domain.peer.model.message.factory;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import jtorrent.domain.peer.model.message.KeepAlive;
import jtorrent.domain.peer.model.message.PeerMessage;
import jtorrent.domain.peer.model.message.typed.Bitfield;
import jtorrent.domain.peer.model.message.typed.Cancel;
import jtorrent.domain.peer.model.message.typed.Choke;
import jtorrent.domain.peer.model.message.typed.Have;
import jtorrent.domain.peer.model.message.typed.Interested;
import jtorrent.domain.peer.model.message.typed.MessageType;
import jtorrent.domain.peer.model.message.typed.NotInterested;
import jtorrent.domain.peer.model.message.typed.Piece;
import jtorrent.domain.peer.model.message.typed.Port;
import jtorrent.domain.peer.model.message.typed.Request;
import jtorrent.domain.peer.model.message.typed.TypedPeerMessage;
import jtorrent.domain.peer.model.message.typed.Unchoke;

public class PeerMessageUnpacker {

    private PeerMessageUnpacker() {
    }

    /**
     * Unpacks a message from a byte array.
     *
     * @param messageBytes the bytes to unpack.
     *                     This should contain the bytes of the complete message excluding the length prefix.
     *                     Cannot be null.
     * @return the unpacked message
     */
    public static PeerMessage unpack(byte[] messageBytes) {
        requireNonNull(messageBytes);

        if (messageBytes.length == 0) {
            return new KeepAlive();
        }

        return unpackTypedMessage(messageBytes);
    }

    private static PeerMessage unpackTypedMessage(byte[] messageBytes) {
        MessageType messageType = MessageType.fromValue(messageBytes[0]);
        byte[] payload = new byte[messageBytes.length - 1];
        System.arraycopy(messageBytes, 1, payload, 0, payload.length);
        return unpackTypedMessage(messageType, payload);
    }

    private static TypedPeerMessage unpackTypedMessage(MessageType messageType, byte[] payload) {
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
