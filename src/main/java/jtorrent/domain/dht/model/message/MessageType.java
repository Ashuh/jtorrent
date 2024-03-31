package jtorrent.domain.dht.model.message;

public enum MessageType {

    QUERY('q'),
    RESPONSE('r'),
    ERROR('e');

    private final char value;

    MessageType(char value) {
        this.value = value;
    }

    public static MessageType fromValue(char value) {
        switch (value) {
        case 'q':
            return QUERY;
        case 'r':
            return RESPONSE;
        case 'e':
            return ERROR;
        default:
            throw new IllegalArgumentException("Unknown message type: " + value);
        }
    }

    public char getValue() {
        return value;
    }
}
