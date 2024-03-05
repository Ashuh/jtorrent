package jtorrent.presentation;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jtorrent.presentation.view.MainWindow;
import jtorrent.presentation.viewmodel.ViewModel;

public class UiManager {

    private final Stage primaryStage;

    public UiManager(Stage primaryStage, ViewModel viewModel) {
        this.primaryStage = primaryStage;
        MainWindow mainWindow = new MainWindow();
        mainWindow.setViewModel(viewModel);
        Scene scene = new Scene(mainWindow, 1200, 1000);
        primaryStage.setTitle("JTorrent");
        primaryStage.setScene(scene);
        setTheme(new PrimerDark());
    }

    public static void setTheme(Theme theme) {
        Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
    }

    public void show() {
        primaryStage.show();
    }
}
