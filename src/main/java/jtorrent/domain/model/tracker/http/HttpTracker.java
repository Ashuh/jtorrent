package jtorrent.domain.model.tracker.http;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import com.dampcake.bencode.BencodeInputStream;

import jtorrent.domain.Constants;
import jtorrent.domain.model.torrent.Torrent;
import jtorrent.domain.model.tracker.AnnounceResponse;
import jtorrent.domain.model.tracker.Event;
import jtorrent.domain.model.tracker.Tracker;
import jtorrent.domain.model.tracker.http.request.HttpAnnounceRequest;
import jtorrent.domain.model.tracker.http.response.HttpAnnounceResponse;

public class HttpTracker extends Tracker {

    private static final Logger LOGGER = System.getLogger(HttpTracker.class.getName());
    private static final String KEY_FAILURE_REASON = "failure reason";

    private final URI uri;

    public HttpTracker(URI uri) {
        this.uri = uri;
    }

    @Override
    public AnnounceResponse announce(Torrent torrent, Event event) throws IOException {
        HttpAnnounceRequest request = new HttpAnnounceRequest.Builder()
                .setInfohash(torrent.getInfoHash())
                .setPeerId(Constants.PEER_ID)
                .setPort(Constants.PORT)
                .setUploaded(torrent.getUploaded())
                .setDownloaded(torrent.getDownloaded())
                .setLeft(torrent.getLeft())
                .setEvent(event)
                .build();

        Map<String, String> requestParams = request.toMap();

        String encodedUrl = requestParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + encodeValue(e.getValue()))
                .collect(Collectors.joining("&", uri.toString() + "?", ""));

        LOGGER.log(Level.TRACE, "Encoded URL: {0}", encodedUrl);

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

    private String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.ISO_8859_1);
    }
}
