package jtorrent.presentation.view.fxml;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class JTorrentFxmlLoader {

    private final FXMLLoader fxmlLoader = new FXMLLoader();

    public <T extends Node> T load(String fileName) throws IOException {
        URL url = getClass().getResource(fileName);

        if (url == null) {
            throw new IOException("Could not find " + fileName);
        }

        fxmlLoader.setLocation(url);
        return fxmlLoader.load();
    }

    public <T> T getController() {
        return fxmlLoader.getController();
    }
}
