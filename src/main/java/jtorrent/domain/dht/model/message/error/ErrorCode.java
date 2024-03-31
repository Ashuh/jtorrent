package jtorrent.domain.dht.model.message.error;

public enum ErrorCode {

    GENERIC_ERROR(201),
    SERVER_ERROR(202),
    PROTOCOL_ERROR(203),
    METHOD_UNKNOWN(204);

    private final int value;

    ErrorCode(int value) {
        this.value = value;
    }

    public static ErrorCode fromValue(int value) {
        switch (value) {
        case 201:
            return GENERIC_ERROR;
        case 202:
            return SERVER_ERROR;
        case 203:
            return PROTOCOL_ERROR;
        case 204:
            return METHOD_UNKNOWN;
        default:
            throw new IllegalArgumentException("Unknown error value: " + value);
        }
    }

    public int getValue() {
        return value;
    }
}
