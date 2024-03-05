package jtorrent.presentation.view;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class PeerInputDialog extends Dialog<String> {

    private final PeerInputView peerInputView;

    public PeerInputDialog() {
        peerInputView = new PeerInputView();
        setDialogPane(peerInputView);
        setTitle("Add Peer");
        setResultConverter(this::convertResult);
    }

    private String convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            return peerInputView.getSocketAddress();
        } else {
            return null;
        }
    }
}
