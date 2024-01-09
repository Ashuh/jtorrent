package jtorrent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.util.logging.LogManager;

import javafx.application.Application;
import jtorrent.presentation.JTorrent;

public class Main {

    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try (InputStream inputStream = Main.class.getResourceAsStream("jul.properties")) {
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (final IOException e) {
            LOGGER.log(Level.ERROR, "Could not load log configuration", e);
        }

        Application.launch(JTorrent.class, args);
    }
}
