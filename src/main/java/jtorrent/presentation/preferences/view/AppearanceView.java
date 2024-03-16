package jtorrent.presentation.preferences.view;

import java.io.IOException;
import java.util.List;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;

public class AppearanceView extends GridPane {

    private static final List<Theme> THEMES =
            List.of(new PrimerDark(), new PrimerLight(), new NordDark(), new NordLight(),
                    new CupertinoDark(), new CupertinoLight(), new Dracula());

    @FXML
    private ChoiceBox<Theme> themeChoiceBox;

    public AppearanceView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @FXML
    public void initialize() {
        themeChoiceBox.setItems(FXCollections.observableArrayList(THEMES));
        themeChoiceBox.setConverter(new ThemeStringConverter());
        themeChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)
                -> Application.setUserAgentStylesheet(newValue.getUserAgentStylesheet()));
    }

    private static class ThemeStringConverter extends StringConverter<Theme> {
        @Override
        public String toString(Theme object) {
            if (object == null) {
                return "";
            }
            return object.getName();
        }

        @Override
        public Theme fromString(String string) {
            return switch (string) {
                case "Primer Dark" -> new PrimerDark();
                case "Primer Light" -> new PrimerLight();
                case "Nord Dark" -> new NordDark();
                case "Nord Light" -> new NordLight();
                case "Dracula" -> new Dracula();
                default -> null;
            };
        }
    }
}
