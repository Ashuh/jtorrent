package jtorrent.peer.domain.model.message.typed;

public abstract class NoPayloadTypedMessage extends TypedPeerMessage {

    @Override
    protected int getPayloadSize() {
        return 0;
    }

    @Override
    protected byte[] getPayload() {
        return new byte[0];
    }

    @Override
    protected String getPayloadString() {
        return "";
    }
}
