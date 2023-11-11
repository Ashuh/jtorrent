package jtorrent.peer.domain.model.message.typed;

public class Unchoke extends NoPayloadTypedMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.UNCHOKE;
    }
}
