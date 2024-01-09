package jtorrent.domain.tracker.model;

/**
 * Represents the event parameter used in tracker announcements.
 *
 * @see <a href="https://www.bittorrent.org/beps/bep_0003.html">BEP 3 - The BitTorrent Protocol Specification</a>
 * @see <a href="https://www.bittorrent.org/beps/bep_0015.html">BEP 15 - UDP Tracker Protocol for BitTorrent</a>
 */
public enum Event {

    NONE(0, "empty"),
    /**
     * Used when a download is completed. Not sent if the file was complete when started.
     */
    COMPLETED(1, "completed"),
    /**
     * Used when a download first begins
     */
    STARTED(2, "started"),
    /**
     * Used when a download is stopped
     */
    STOPPED(3, "stopped");

    /**
     * The value of the event parameter used in the UDP tracker protocol.
     */
    private final int udpValue;
    /**
     * The value of the event parameter used in the HTTP tracker protocol.
     */
    private final String httpValue;

    Event(int udpValue, String httpValue) {
        this.udpValue = udpValue;
        this.httpValue = httpValue;
    }

    /**
     * Gets the integer value representing the event parameter used in the UDP tracker protocol.
     *
     * @return the integer value representing the event parameter used in the UDP tracker protocol
     */
    public int getUdpValue() {
        return udpValue;
    }

    /**
     * Gets the string value representing the event parameter used in the HTTP tracker protocol.
     *
     * @return the string value representing the event parameter used in the HTTP tracker protocol
     */
    public String getHttpValue() {
        return httpValue;
    }
}
