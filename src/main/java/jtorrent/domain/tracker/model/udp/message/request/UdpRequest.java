package jtorrent.domain.tracker.model.udp.message.request;

import jtorrent.domain.tracker.model.udp.message.UdpMessage;

public abstract class UdpRequest extends UdpMessage {

    protected UdpRequest() {
        super(generateTransactionId());
    }

    private static int generateTransactionId() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    public abstract byte[] pack();
}
