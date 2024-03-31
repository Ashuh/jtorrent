package jtorrent.domain.peer.model.message.typed;

public class Interested extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.INTERESTED;
    }
}
