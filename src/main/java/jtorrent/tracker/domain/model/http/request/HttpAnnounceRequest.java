package jtorrent.tracker.domain.model.http.request;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jtorrent.common.domain.util.Sha1Hash;
import jtorrent.tracker.domain.model.Event;

public class HttpAnnounceRequest {

    private static final String KEY_INFO_HASH = "info_hash";
    private static final String KEY_PEER_ID = "peer_id";
    private static final String KEY_DOWNLOADED = "downloaded";
    private static final String KEY_LEFT = "left";
    private static final String KEY_UPLOADED = "uploaded";
    private static final String KEY_PORT = "port";
    private static final String KEY_EVENT = "event";
    private static final String KEY_IP = "ip";
    private static final String KEY_NUM_WANT = "numwant";

    private final Sha1Hash infohash;
    private final String peerId;
    private final long downloaded;
    private final long left;
    private final long uploaded;
    private final int port;
    /**
     * Optional
     */
    private final Event event;
    /**
     * Optional
     */
    private final Integer ip;
    /**
     * Optional
     */
    private final Integer numWant;

    private HttpAnnounceRequest(Sha1Hash infohash, String peerId, long downloaded, long left, long uploaded, int port,
            Event event, Integer ip, Integer numWant) {
        this.infohash = requireNonNull(infohash);
        this.peerId = requireNonNull(peerId);
        this.downloaded = downloaded;
        this.left = left;
        this.uploaded = uploaded;
        this.port = port;
        this.event = event;
        this.ip = ip;
        this.numWant = numWant;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_INFO_HASH, new String(infohash.getBytes(), StandardCharsets.ISO_8859_1));
        map.put(KEY_PEER_ID, peerId);
        map.put(KEY_DOWNLOADED, String.valueOf(downloaded));
        map.put(KEY_LEFT, String.valueOf(left));
        map.put(KEY_UPLOADED, String.valueOf(uploaded));
        map.put(KEY_PORT, String.valueOf(port));
        if (event != null) {
            map.put(KEY_EVENT, event.toString().toLowerCase());
        }
        if (ip != null) {
            map.put(KEY_IP, String.valueOf(ip));
        }
        if (numWant != null) {
            map.put(KEY_NUM_WANT, String.valueOf(numWant));
        }
        return map;
    }

    public static class Builder {

        private Sha1Hash infohash;
        private String peerId;
        private Long downloaded;
        private Long left;
        private Long uploaded;
        private Integer port;
        private Event event;
        private Integer ip;
        private Integer numWant;

        public Builder setInfohash(Sha1Hash infohash) {
            this.infohash = infohash;
            return this;
        }

        public Builder setPeerId(String peerId) {
            this.peerId = peerId;
            return this;
        }

        public Builder setDownloaded(long downloaded) {
            this.downloaded = downloaded;
            return this;
        }

        public Builder setLeft(long left) {
            this.left = left;
            return this;
        }

        public Builder setUploaded(long uploaded) {
            this.uploaded = uploaded;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setEvent(Event event) {
            this.event = event;
            return this;
        }

        public Builder setIp(int ip) {
            this.ip = ip;
            return this;
        }

        public Builder setNumWant(int numWant) {
            this.numWant = numWant;
            return this;
        }

        public HttpAnnounceRequest build() {
            if (infohash == null || peerId == null || downloaded == null
                    || left == null || uploaded == null || port == null) {
                throw new IllegalStateException("Not all compulsory fields are set");
            }

            return new HttpAnnounceRequest(infohash, peerId, downloaded, left, uploaded, port, event, ip, numWant);
        }
    }
}
