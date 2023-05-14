package jtorrent.domain.model.peer.message.typed;

public class NotInterested extends NoPayloadTypedMessage {

    @Override
    protected MessageType getMessageType() {
        return MessageType.NOT_INTERESTED;
    }
}
