package jtorrent.domain.model.peer.message.typed;

public class Unchoke extends NoPayloadTypedMessage {

    @Override
    protected MessageType getMessageType() {
        return MessageType.UNCHOKE;
    }
}
