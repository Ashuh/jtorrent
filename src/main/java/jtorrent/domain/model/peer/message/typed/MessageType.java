package jtorrent.domain.model.peer.message.typed;

public enum MessageType {

    CHOKE((byte) 0),
    UNCHOKE((byte) 1),
    INTERESTED((byte) 2),
    NOT_INTERESTED((byte) 3),
    HAVE((byte) 4),
    BITFIELD((byte) 5),
    REQUEST((byte) 6),
    PIECE((byte) 7),
    CANCEL((byte) 8),
    PORT((byte) 9);

    private final byte value;

    MessageType(byte value) {
        this.value = value;
    }

    public static MessageType fromValue(byte value) {
        switch (value) {
        case 0:
            return CHOKE;
        case 1:
            return UNCHOKE;
        case 2:
            return INTERESTED;
        case 3:
            return NOT_INTERESTED;
        case 4:
            return HAVE;
        case 5:
            return BITFIELD;
        case 6:
            return REQUEST;
        case 7:
            return PIECE;
        case 8:
            return CANCEL;
        case 9:
            return PORT;
        default:
            throw new IllegalArgumentException("Invalid message type: " + value);
        }
    }

    public byte getValue() {
        return value;
    }
}
