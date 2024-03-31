package jtorrent.domain.tracker.model.udp.message;

public abstract class UdpMessage {

    protected final int transactionId;

    protected UdpMessage(int transactionId) {
        this.transactionId = transactionId;
    }

    public boolean hasMatchingTransactionId(UdpMessage message) {
        return this.transactionId == message.getTransactionId();
    }

    public int getTransactionId() {
        return transactionId;
    }
}
