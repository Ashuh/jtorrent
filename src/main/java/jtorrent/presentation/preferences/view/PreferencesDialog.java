package jtorrent.presentation.preferences.view;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class PreferencesDialog extends Dialog<String> {

    private final PreferencesView preferencesView;

    public PreferencesDialog() {
        preferencesView = new PreferencesView();
        setDialogPane(preferencesView);
        setTitle("Preferences");
        setResizable(true);
        setResultConverter(this::convertResult);
    }

    private String convertResult(ButtonType buttonType) {
        return null;
    }
}
