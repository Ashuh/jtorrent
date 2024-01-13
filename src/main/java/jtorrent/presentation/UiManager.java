package jtorrent.presentation;

import java.io.IOException;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jtorrent.presentation.view.MainWindow;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;
import jtorrent.presentation.viewmodel.ViewModel;

public class UiManager {

    private final Stage primaryStage;

    public UiManager(Stage primaryStage, ViewModel viewModel) {
        this.primaryStage = primaryStage;

        try {
            JTorrentFxmlLoader loader = new JTorrentFxmlLoader();
            Parent root = loader.load("MainWindow.fxml");
            Scene scene = new Scene(root, 1200, 1000);
            MainWindow mainWindow = loader.getController();
            mainWindow.setViewModel(viewModel);
            primaryStage.setTitle("JTorrent");
            primaryStage.setScene(scene);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void show() {
        primaryStage.show();
    }
}
