package jtorrent.domain.model.peer.message.typed;

public class NotInterested extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.NOT_INTERESTED;
    }
}
