package jtorrent.presentation.view;

import java.io.IOException;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;

public class PreferencesDialog extends Dialog<String> {

    private final PreferencesView preferencesView;

    public PreferencesDialog() {
        try {
            JTorrentFxmlLoader loader = new JTorrentFxmlLoader();
            DialogPane dialogPane = loader.load("PreferencesView.fxml");
            preferencesView = loader.getController();
            setDialogPane(dialogPane);
            setTitle("Preferences");
            setResizable(true);
            setResultConverter(this::convertResult);

        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private String convertResult(ButtonType buttonType) {
        return null;
    }
}
