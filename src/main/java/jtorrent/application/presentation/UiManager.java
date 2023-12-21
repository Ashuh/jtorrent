package jtorrent.application.presentation;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jtorrent.application.presentation.view.MainWindow;
import jtorrent.application.presentation.viewmodel.ViewModel;

public class UiManager {

    private final Stage primaryStage;

    public UiManager(Stage primaryStage, ViewModel viewModel) {
        this.primaryStage = primaryStage;

        String fileName = "fxml/MainWindow.fxml";
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fileName));
        try {
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1200, 1000);
            MainWindow mainWindow = fxmlLoader.getController();
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
