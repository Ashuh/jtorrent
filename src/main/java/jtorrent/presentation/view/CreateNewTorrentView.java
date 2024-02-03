package jtorrent.presentation.view;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import jtorrent.presentation.model.UiNewTorrent;

public class CreateNewTorrentView {

    @FXML
    private ComboBox<File> source;
    @FXML
    private Button addDirectory;
    @FXML
    private Button addFile;
    @FXML
    private ChoiceBox<String> pieceSize;

    @FXML
    private void initialize() {
        source.setConverter(new StringConverter<>() {
            @Override
            public String toString(File file) {
                if (file == null) {
                    return "";
                }
                return file.getAbsolutePath();
            }

            @Override
            public File fromString(String s) {
                return new File(s);
            }
        });

        addDirectory.setOnMouseClicked(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select a folder");
            File selectedDir = directoryChooser.showDialog(addDirectory.getScene().getWindow());
            if (selectedDir != null) {
                source.setValue(selectedDir);
            }
        });

        addFile.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a file");
            File selectedDir = fileChooser.showOpenDialog(addFile.getScene().getWindow());
            if (selectedDir != null) {
                source.setValue(selectedDir);
            }
        });

        pieceSize.getItems().addAll("512", "1024", "2048", "4096");
    }

    public UiNewTorrent getNewTorrent() {
        return new UiNewTorrent();
    }
}
