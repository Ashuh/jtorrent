package jtorrent.domain.common.util.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Markers {

    public static final Marker TORRENT = MarkerFactory.getMarker("TORRENT");
    public static final Marker PEER = MarkerFactory.getMarker("PEER");
    public static final Marker TRACKER = MarkerFactory.getMarker("TRACKER");
    public static final Marker INBOUND = MarkerFactory.getMarker("INBOUND");
    public static final Marker LSD = MarkerFactory.getMarker("LSD");
    public static final Marker DHT = MarkerFactory.getMarker("DHT");

    private Markers() {
    }
}
