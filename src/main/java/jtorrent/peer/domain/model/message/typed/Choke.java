package jtorrent.peer.domain.model.message.typed;

public class Choke extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.CHOKE;
    }
}
