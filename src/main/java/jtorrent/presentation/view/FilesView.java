package jtorrent.presentation.view;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jtorrent.presentation.component.DataStatusBarTableCell;
import jtorrent.presentation.model.UiFileInfo;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;
import jtorrent.presentation.viewmodel.ViewModel;

public class FilesView extends TableView<UiFileInfo> {

    private final ObjectProperty<ViewModel> viewModel = new SimpleObjectProperty<>();
    @FXML
    private TableColumn<UiFileInfo, String> path;
    @FXML
    private TableColumn<UiFileInfo, String> size;
    @FXML
    private TableColumn<UiFileInfo, String> done;
    @FXML
    private TableColumn<UiFileInfo, String> percentDone;
    @FXML
    private TableColumn<UiFileInfo, Integer> firstPiece;
    @FXML
    private TableColumn<UiFileInfo, Integer> numPieces;
    @FXML
    private TableColumn<UiFileInfo, DataStatusBarTableCell.State> pieces;
    @FXML
    private TableColumn<UiFileInfo, String> priority;
    @FXML
    private TableColumn<UiFileInfo, String> mode;
    @FXML
    private TableColumn<UiFileInfo, String> rate;
    @FXML
    private TableColumn<UiFileInfo, String> resolution;
    @FXML
    private TableColumn<UiFileInfo, String> duration;
    @FXML
    private TableColumn<UiFileInfo, String> streamable;
    @FXML
    private TableColumn<UiFileInfo, String> hasHeader;
    @FXML
    private TableColumn<UiFileInfo, String> codecs;

    public FilesView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @FXML
    public void initialize() {
        itemsProperty().bind(viewModel.flatMap(ViewModel::getFileInfos));

        path.setCellValueFactory(param -> param.getValue().pathProperty());
        size.setCellValueFactory(param -> param.getValue().sizeProperty());
        done.setCellValueFactory(param -> param.getValue().doneProperty());
        percentDone.setCellValueFactory(param -> param.getValue().percentDoneProperty());
        firstPiece.setCellValueFactory(param -> param.getValue().firstPieceProperty().asObject());
        numPieces.setCellValueFactory(param -> param.getValue().numPiecesProperty().asObject());
        pieces.setCellValueFactory(param -> Bindings.createObjectBinding(() ->
                new DataStatusBarTableCell.State(
                        param.getValue().numPiecesProperty(),
                        param.getValue().downloadedPiecesProperty()
                ), param.getValue().downloadedPiecesProperty(), param.getValue().numPiecesProperty()));
        pieces.setCellFactory(column -> new DataStatusBarTableCell());
        priority.setCellValueFactory(param -> param.getValue().priorityProperty());
        mode.setCellValueFactory(param -> param.getValue().modeProperty());
        rate.setCellValueFactory(param -> param.getValue().rateProperty());
        resolution.setCellValueFactory(param -> param.getValue().resolutionProperty());
        duration.setCellValueFactory(param -> param.getValue().durationProperty());
        streamable.setCellValueFactory(param -> param.getValue().streamableProperty());
        hasHeader.setCellValueFactory(param -> param.getValue().hasHeaderProperty());
        codecs.setCellValueFactory(param -> param.getValue().codecsProperty());
    }

    public ObjectProperty<ViewModel> viewModelProperty() {
        return viewModel;
    }
}
