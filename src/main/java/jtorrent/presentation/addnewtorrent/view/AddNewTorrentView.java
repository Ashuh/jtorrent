package jtorrent.presentation.addnewtorrent.view;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.io.File;
import java.io.IOException;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import jtorrent.presentation.addnewtorrent.model.UiFileInfo;
import jtorrent.presentation.addnewtorrent.viewmodel.AddNewTorrentViewModel;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;

public class AddNewTorrentView extends DialogPane {

    private final ObjectProperty<AddNewTorrentViewModel> viewModel = new SimpleObjectProperty<>();
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
    private TreeTableView<UiFileInfo> tableView;
    @FXML
    private TreeTableColumn<UiFileInfo, String> fileName;
    @FXML
    private TreeTableColumn<UiFileInfo, String> fileSize;

    public AddNewTorrentView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void setViewModel(AddNewTorrentViewModel viewModel) {
        this.viewModel.set(viewModel);
    }

    public String getSaveDirectory() {
        return saveDirectoryInput.getText();
    }

    public String getName() {
        return nameInput.getText();
    }

    @FXML
    public void initialize() {
        viewModel.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                nameInput.textProperty().unbindBidirectional(oldValue.nameProperty());
                saveDirectoryInput.textProperty().unbindBidirectional(oldValue.saveDirectoryProperty());
            }
            if (newValue != null) {
                nameInput.textProperty().bindBidirectional(newValue.nameProperty());
                saveDirectoryInput.textProperty().bindBidirectional(newValue.saveDirectoryProperty());
            }
        });

        name.textProperty().bind(viewModel.flatMap(AddNewTorrentViewModel::nameProperty));
        comment.textProperty().bind(viewModel.flatMap(AddNewTorrentViewModel::commentProperty));
        size.textProperty().bind(viewModel.flatMap(AddNewTorrentViewModel::sizeProperty));
        date.textProperty().bind(viewModel.flatMap(AddNewTorrentViewModel::dateProperty));
        fileName.setCellValueFactory(cellData -> cellData.getValue().getValue().name());
        fileSize.setCellValueFactory(cellData -> cellData.getValue().getValue().size());
        tableView.rootProperty().bind(viewModel.flatMap(AddNewTorrentViewModel::getFiles));
        browseButton.disableProperty().bind(viewModel.isNull());
        browseButton.onMouseClickedProperty().bind(viewModel.map(BrowseButtonMouseEventHandler::new));
    }

    private class BrowseButtonMouseEventHandler implements EventHandler<MouseEvent> {

        private final AddNewTorrentViewModel vm;

        private BrowseButtonMouseEventHandler(AddNewTorrentViewModel vm) {
            this.vm = requireNonNull(vm);
        }

        @Override
        public void handle(MouseEvent event) {

            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose where to download '" + vm.getName() + "' to");
            File dir = new File(vm.getSaveDirectory());
            // TODO: strip invalid path until a valid path is found
            if (dir.exists()) {
                directoryChooser.setInitialDirectory(dir);
            }
            File selectedDir = directoryChooser.showDialog(getScene().getWindow());
            if (selectedDir != null) {
                saveDirectoryInput.setText(selectedDir.getAbsolutePath());
            }
        }
    }
}
