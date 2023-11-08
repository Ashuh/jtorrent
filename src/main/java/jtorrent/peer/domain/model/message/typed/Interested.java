package jtorrent.peer.domain.model.message.typed;

public class Interested extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.INTERESTED;
    }
}
