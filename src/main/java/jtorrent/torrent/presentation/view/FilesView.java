package jtorrent.torrent.presentation.view;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jtorrent.torrent.presentation.UiFileInfo;

public class FilesView implements Initializable {

    @FXML
    private TableView<UiFileInfo> tableView;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        path.setCellValueFactory(cd -> cd.getValue().pathProperty());
        size.setCellValueFactory(cd -> cd.getValue().sizeProperty());
        done.setCellValueFactory(cd -> cd.getValue().doneProperty());
        percentDone.setCellValueFactory(cd -> cd.getValue().percentDoneProperty());
        firstPiece.setCellValueFactory(cd -> cd.getValue().firstPieceProperty().asObject());
        numPieces.setCellValueFactory(cd -> cd.getValue().numPiecesProperty().asObject());
        pieces.setCellValueFactory(cd -> Bindings.createObjectBinding(() ->
                new DataStatusBarTableCell.State(
                        cd.getValue().numPiecesProperty(),
                        cd.getValue().downloadedPiecesProperty()
                ), cd.getValue().downloadedPiecesProperty(), cd.getValue().numPiecesProperty()));
        pieces.setCellFactory(column -> new DataStatusBarTableCell());
        priority.setCellValueFactory(cd -> cd.getValue().priorityProperty());
        mode.setCellValueFactory(cd -> cd.getValue().modeProperty());
        rate.setCellValueFactory(cd -> cd.getValue().rateProperty());
        resolution.setCellValueFactory(cd -> cd.getValue().resolutionProperty());
        duration.setCellValueFactory(cd -> cd.getValue().durationProperty());
        streamable.setCellValueFactory(cd -> cd.getValue().streamableProperty());
        hasHeader.setCellValueFactory(cd -> cd.getValue().hasHeaderProperty());
        codecs.setCellValueFactory(cd -> cd.getValue().codecsProperty());
    }

    public ObjectProperty<ObservableList<UiFileInfo>> itemsProperty() {
        return tableView.itemsProperty();
    }
}
