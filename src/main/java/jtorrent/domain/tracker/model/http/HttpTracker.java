package jtorrent.domain.tracker.model.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dampcake.bencode.BencodeInputStream;

import jtorrent.domain.common.Constants;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.logging.Markers;
import jtorrent.domain.tracker.model.Event;
import jtorrent.domain.tracker.model.Tracker;
import jtorrent.domain.tracker.model.http.request.HttpAnnounceRequest;
import jtorrent.domain.tracker.model.http.response.HttpAnnounceResponse;

public class HttpTracker implements Tracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpTracker.class);
    private static final String KEY_FAILURE_REASON = "failure reason";

    private final URI uri;

    public HttpTracker(URI uri) {
        this.uri = uri;
    }

    @Override
    public HttpAnnounceResponse announce(Sha1Hash infoHash, long downloaded, long left, long uploaded, Event event)
            throws IOException {
        HttpAnnounceRequest request = new HttpAnnounceRequest.Builder()
                .setInfohash(infoHash)
                .setPeerId(Constants.PEER_ID)
                .setPort(Constants.PORT)
                .setUploaded(uploaded)
                .setDownloaded(downloaded)
                .setLeft(left)
                .setEvent(event)
                .build();

        Map<String, String> requestParams = request.toMap();

        String encodedUrl = requestParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + encodeValue(e.getValue()))
                .collect(Collectors.joining("&", uri.toString() + "?", ""));

        LOGGER.trace(Markers.TRACKER, "Encoded URL: {}", encodedUrl);

        URL url = new URL(encodedUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("Http request failed with response code: "
                    + responseCode
                    + " "
                    + connection.getResponseMessage());
        }

        BencodeInputStream bis = new BencodeInputStream(connection.getInputStream(), StandardCharsets.UTF_8);
        Map<String, Object> map = bis.readDictionary();
        connection.disconnect();

        if (map.containsKey(KEY_FAILURE_REASON)) {
            String failureReason = (String) map.get(KEY_FAILURE_REASON);
            throw new IOException("Announce failed with reason: " + failureReason);
        }

        return HttpAnnounceResponse.fromMap(map);
    }

    @Override
    public URI getUri() {
        return uri;
    }

    private String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.ISO_8859_1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HttpTracker that = (HttpTracker) o;
        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
