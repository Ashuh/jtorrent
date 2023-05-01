package jtorrent.domain.model.peer.message.typed;

public class Interested extends NoPayloadTypedMessage {

    @Override
    protected MessageType getMessageType() {
        return MessageType.INTERESTED;
    }
}
