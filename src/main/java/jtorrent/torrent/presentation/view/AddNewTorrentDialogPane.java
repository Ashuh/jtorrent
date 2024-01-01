package jtorrent.torrent.presentation.view;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import jtorrent.application.presentation.UiManager;
import jtorrent.torrent.presentation.UiTorrentContents;

public class AddNewTorrentDialogPane extends DialogPane {

    @FXML
    private TextField nameInput;
    @FXML
    private TextField saveDirectoryInput;
    @FXML
    private Button browseButton;
    @FXML
    private Text name;
    @FXML
    private Text comment;
    @FXML
    private Text size;
    @FXML
    private Text date;
    @FXML
    private TreeTableView<UiTorrentContents.FileInfo> tableView;
    @FXML
    private TreeTableColumn<UiTorrentContents.FileInfo, String> fileName;
    @FXML
    private TreeTableColumn<UiTorrentContents.FileInfo, String> fileSize;

    public AddNewTorrentDialogPane(UiTorrentContents torrentContents) {
        FXMLLoader fxmlLoader = new FXMLLoader(UiManager.class.getResource("fxml/AddNewTorrentDialog.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        nameInput.textProperty().bindBidirectional(torrentContents.nameProperty());
        saveDirectoryInput.textProperty().bindBidirectional(torrentContents.saveDirectoryProperty());
        name.textProperty().bind(torrentContents.nameProperty());
        comment.textProperty().bind(torrentContents.commentProperty());
        size.textProperty().bind(torrentContents.sizeProperty());
        date.textProperty().bind(torrentContents.dateProperty());
        fileName.setCellValueFactory(cellData -> cellData.getValue().getValue().name());
        fileSize.setCellValueFactory(cellData -> cellData.getValue().getValue().size());
        tableView.setRoot(torrentContents.getFiles());

        browseButton.setOnMouseClicked(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose where to download '" + torrentContents.getName() + "' to");
            File dir = new File(torrentContents.getSaveDirectory());
            // TODO: strip invalid path until a valid path is found
            if (dir.exists()) {
                directoryChooser.setInitialDirectory(dir);
            }
            File selectedDir = directoryChooser.showDialog(getScene().getWindow());
            if (selectedDir != null) {
                saveDirectoryInput.setText(selectedDir.getAbsolutePath());
            }
        });
    }

    public String getSaveDirectory() {
        return saveDirectoryInput.getText();
    }

    public String getName() {
        return nameInput.getText();
    }
}
