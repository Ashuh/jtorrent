package jtorrent.application.presentation;

import javafx.scene.Scene;
import javafx.stage.Stage;
import jtorrent.application.presentation.view.MainWindow;
import jtorrent.application.presentation.viewmodel.ViewModel;

public class UiManager {

    private final Stage primaryStage;

    public UiManager(Stage primaryStage, ViewModel viewModel) {
        this.primaryStage = primaryStage;
        MainWindow mainWindow = new MainWindow(viewModel);
        Scene scene = new Scene(mainWindow, 1200, 1000);
        primaryStage.setTitle("JTorrent");
        primaryStage.setScene(scene);
    }

    public void show() {
        primaryStage.show();
    }
}
