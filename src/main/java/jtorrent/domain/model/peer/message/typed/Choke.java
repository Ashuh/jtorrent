package jtorrent.domain.model.peer.message.typed;

public class Choke extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.CHOKE;
    }
}
