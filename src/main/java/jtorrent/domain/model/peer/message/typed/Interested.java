package jtorrent.domain.model.peer.message.typed;

public class Interested extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.INTERESTED;
    }
}
