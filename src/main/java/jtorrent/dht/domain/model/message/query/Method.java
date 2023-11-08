package jtorrent.dht.domain.model.message.query;

public enum Method {

    PING("ping"),
    FIND_NODE("find_node"),
    GET_PEERS("get_peers"),
    ANNOUNCE_PEER("announce_peer");

    private final String value;

    Method(String value) {
        this.value = value;
    }

    public static Method fromValue(String string) {
        switch (string) {
        case "ping":
            return PING;
        case "find_node":
            return FIND_NODE;
        case "get_peers":
            return GET_PEERS;
        case "announce_peer":
            return ANNOUNCE_PEER;
        default:
            throw new IllegalArgumentException("Unknown method: " + string);
        }
    }

    public String getValue() {
        return value;
    }
}
