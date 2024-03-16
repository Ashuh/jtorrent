package jtorrent.presentation.createnewtorrent.view;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;

public class CreateNewTorrentView extends DialogPane {

    @FXML
    private ComboBox<File> source;
    @FXML
    private Button addDirectory;
    @FXML
    private Button addFile;
    @FXML
    private TextArea trackers;
    @FXML
    private TextField comment;
    @FXML
    private ChoiceBox<String> pieceSize;

    public CreateNewTorrentView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

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
            File selectedDir = directoryChooser.showDialog(getScene().getWindow());
            if (selectedDir != null) {
                source.setValue(selectedDir);
            }
        });

        addFile.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a file");
            File selectedDir = fileChooser.showOpenDialog(getScene().getWindow());
            if (selectedDir != null) {
                source.setValue(selectedDir);
            }
        });

        pieceSize.getItems().addAll("512", "1024", "2048", "4096");

        // TODO: temporary placeholder
        String placeholder = """
                udp://tracker.openbittorrent.com:80/announce

                udp://tracker.opentrackr.org:1337/announce
                """;
        trackers.setText(placeholder);
    }

    public File getSource() {
        return source.getValue();
    }

    public int getPieceSize() {
        return Integer.parseInt(pieceSize.getValue()) * 1024;
    }

    public String getTrackers() {
        return trackers.getText();
    }

    public String getComment() {
        return comment.getText();
    }
}
