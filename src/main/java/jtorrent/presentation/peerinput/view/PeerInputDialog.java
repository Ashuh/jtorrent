package jtorrent.presentation.peerinput.view;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class PeerInputDialog extends Dialog<PeerInputDialog.Result> {

    private final PeerInputView peerInputView;

    public PeerInputDialog() {
        peerInputView = new PeerInputView();
        setDialogPane(peerInputView);
        setTitle("Add Peer");
        setResultConverter(this::convertResult);
    }

    private Result convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            return new Result(peerInputView.getIp(), peerInputView.getPort());
        } else {
            return null;
        }
    }

    public record Result(String ip, String port) {
    }
}
