package jtorrent.presentation.view;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;

public abstract class UiComponent extends StackPane {

    protected UiComponent() {
        String fileName = getClass().getSimpleName() + ".fxml";
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fileName));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
