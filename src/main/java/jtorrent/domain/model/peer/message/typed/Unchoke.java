package jtorrent.domain.model.peer.message.typed;

public class Unchoke extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.UNCHOKE;
    }
}
