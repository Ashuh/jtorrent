package jtorrent.domain.peer.model.message.typed;

public class NotInterested extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.NOT_INTERESTED;
    }
}
