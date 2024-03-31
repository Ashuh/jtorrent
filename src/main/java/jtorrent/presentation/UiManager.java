package jtorrent.presentation;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.Theme;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jtorrent.presentation.main.view.MainView;
import jtorrent.presentation.main.viewmodel.MainViewModel;

public class UiManager {

    private final Stage primaryStage;

    public UiManager(Stage primaryStage, MainViewModel viewModel) {
        this.primaryStage = primaryStage;
        MainView mainView = new MainView();
        mainView.setViewModel(viewModel);
        Scene scene = new Scene(mainView, 1200, 1000);
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
