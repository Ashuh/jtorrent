package jtorrent.domain.model.tracker.udp.message;

public abstract class Request extends UdpMessage {

    protected Request() {
        super(generateTransactionId());
    }

    private static int generateTransactionId() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    public abstract byte[] pack();
}
