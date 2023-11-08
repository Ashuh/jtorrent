package jtorrent.peer.domain.model.message.typed;

public abstract class NoPayloadTypedMessage extends TypedPeerMessage {

    @Override
    protected byte[] getPayload() {
        return new byte[0];
    }
}
