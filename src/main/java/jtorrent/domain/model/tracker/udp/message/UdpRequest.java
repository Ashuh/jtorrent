package jtorrent.domain.model.tracker.udp.message;

public abstract class UdpRequest extends UdpMessage {

    protected UdpRequest() {
        super(generateTransactionId());
    }

    private static int generateTransactionId() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    public abstract byte[] pack();
}
