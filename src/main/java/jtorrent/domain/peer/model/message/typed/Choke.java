package jtorrent.domain.peer.model.message.typed;

public class Choke extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.CHOKE;
    }
}
