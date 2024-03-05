package jtorrent.presentation.view.fxml;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class JTorrentFxmlLoader {

    private JTorrentFxmlLoader() {
    }

    /**
     * Loads a view from its fxml file and sets the view as the root and controller.
     * The view must extend the type of the root defined in the fxml file and have the same class name as the fxml file.
     * This should be called from the constructor of the view.
     *
     * @param view The view to load
     * @throws IOException if the view could not be loaded
     */
    public static void loadView(Node view) throws IOException {
        FXMLLoader loader = createLoader(view);
        loader.load();
    }

    private static FXMLLoader createLoader(Node view) {
        URL url = getFxmlUrl(view);
        FXMLLoader loader = new FXMLLoader(url);
        loader.setRoot(view);
        loader.setController(view);
        return loader;
    }

    private static URL getFxmlUrl(Node view) {
        String fileName = view.getClass().getSimpleName() + ".fxml";
        return JTorrentFxmlLoader.class.getResource(fileName);
    }
}
