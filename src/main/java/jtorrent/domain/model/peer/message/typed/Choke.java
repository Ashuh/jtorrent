package jtorrent.domain.model.peer.message.typed;

public class Choke extends NoPayloadTypedMessage {

    @Override
    protected MessageType getMessageType() {
        return MessageType.CHOKE;
    }
}
