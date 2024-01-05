package jtorrent.torrent.presentation.view;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import jtorrent.torrent.presentation.UiTorrentContents;

public class AddNewTorrentView implements Initializable {

    private final ObjectProperty<UiTorrentContents> torrentContents = new SimpleObjectProperty<>();
    @FXML
    private DialogPane dialogPane;
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

    public void setTorrentContents(UiTorrentContents torrentContents) {
        this.torrentContents.set(torrentContents);
    }

    public String getSaveDirectory() {
        return saveDirectoryInput.getText();
    }

    public String getName() {
        return nameInput.getText();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        torrentContents.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                nameInput.textProperty().unbindBidirectional(oldValue.nameProperty());
                saveDirectoryInput.textProperty().unbindBidirectional(oldValue.saveDirectoryProperty());
            }
            if (newValue != null) {
                nameInput.textProperty().bindBidirectional(newValue.nameProperty());
                saveDirectoryInput.textProperty().bindBidirectional(newValue.saveDirectoryProperty());
            }
        });

        name.textProperty().bind(torrentContents.flatMap(UiTorrentContents::nameProperty));
        comment.textProperty().bind(torrentContents.flatMap(UiTorrentContents::commentProperty));
        size.textProperty().bind(torrentContents.flatMap(UiTorrentContents::sizeProperty));
        date.textProperty().bind(torrentContents.flatMap(UiTorrentContents::dateProperty));
        fileName.setCellValueFactory(cellData -> cellData.getValue().getValue().name());
        fileSize.setCellValueFactory(cellData -> cellData.getValue().getValue().size());
        tableView.rootProperty().bind(torrentContents.map(UiTorrentContents::getFiles));
        browseButton.disableProperty().bind(torrentContents.isNull());

        browseButton.setOnMouseClicked(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose where to download '" + torrentContents.getName() + "' to");
            File dir = new File(torrentContents.get().getSaveDirectory());
            // TODO: strip invalid path until a valid path is found
            if (dir.exists()) {
                directoryChooser.setInitialDirectory(dir);
            }
            File selectedDir = directoryChooser.showDialog(dialogPane.getScene().getWindow());
            if (selectedDir != null) {
                saveDirectoryInput.setText(selectedDir.getAbsolutePath());
            }
        });
    }
}
