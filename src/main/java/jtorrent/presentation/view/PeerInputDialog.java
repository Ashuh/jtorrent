package jtorrent.presentation.view;

import java.io.IOException;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;

public class PeerInputDialog extends Dialog<String> {

    private final PeerInputView peerInputView;

    public PeerInputDialog() {
        JTorrentFxmlLoader loader = new JTorrentFxmlLoader();
        try {
            DialogPane dialogPane = loader.load("PeerInputView.fxml");
            peerInputView = loader.getController();
            setDialogPane(dialogPane);
            setTitle("Add Peer");
            setResultConverter(this::convertResult);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private String convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            return peerInputView.getSocketAddress();
        } else {
            return null;
        }
    }
}
