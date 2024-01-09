package jtorrent.domain.lsd.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import jtorrent.domain.common.util.Sha1Hash;

public class Announce {

    private static final String HEADER = "BT-SEARCH * HTTP/1.1";
    private static final String KEY_HOST = "Host";
    private static final String KEY_PORT = "Port";
    private static final String KEY_INFOHASH = "Infohash";
    private static final String KEY_COOKIE = "cookie";

    private final String host;
    private final int port;
    private final Set<Sha1Hash> infoHashes;
    private final String cookie;

    public Announce(String host, int port, Set<Sha1Hash> infoHashes, String cookie) {
        this.host = requireNonNull(host);
        this.port = port;
        this.infoHashes = requireNonNull(infoHashes);
        this.cookie = cookie;

        if (infoHashes.isEmpty()) {
            throw new IllegalArgumentException("InfoHashes cannot be empty");
        }
    }

    public static Announce fromString(String string) {
        Scanner scanner = new Scanner(string);

        String firstLine = scanner.nextLine();
        if (!firstLine.equals(HEADER)) {
            throw new IllegalArgumentException("Invalid announcement string");
        }

        Map<String, List<String>> keyToValues = new HashMap<>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = line.split(":", 2);
            if (tokens.length != 2) {
                throw new IllegalArgumentException("Invalid announcement string");
            }

            String key = tokens[0].trim();
            String value = tokens[1].trim();
            keyToValues.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        scanner.close();

        String host = keyToValues.getOrDefault(KEY_HOST, Collections.emptyList()).stream()
                .findFirst()
                .orElseThrow();
        int port = keyToValues.getOrDefault(KEY_PORT, Collections.emptyList()).stream()
                .map(Integer::parseInt)
                .findFirst()
                .orElseThrow();
        Set<Sha1Hash> infoHashes = keyToValues.getOrDefault(KEY_INFOHASH, Collections.emptyList()).stream()
                .map(Sha1Hash::fromHexString)
                .collect(Collectors.toSet());
        String cookie = keyToValues.getOrDefault(KEY_COOKIE, Collections.emptyList()).stream()
                .findFirst()
                .orElse(null);

        return new Announce(host, port, infoHashes, cookie);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Set<Sha1Hash> getInfoHashes() {
        return infoHashes;
    }

    public Optional<String> getCookie() {
        return Optional.ofNullable(cookie);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append(HEADER).append("\r\n")
                .append(KEY_HOST).append(": ").append(host).append("\r\n")
                .append(KEY_PORT).append(": ").append(port).append("\r\n");
        for (Sha1Hash infoHash : infoHashes) {
            builder.append(KEY_INFOHASH).append(": ").append(infoHash).append("\r\n");
        }
        if (cookie != null) {
            builder.append(KEY_COOKIE).append(": ").append(cookie).append("\r\n");
        }
        builder.append("\r\n\r\n");
        return builder.toString();
    }
}
