package jtorrent.presentation.view;

import java.io.IOException;
import java.util.BitSet;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import jtorrent.presentation.component.DataStatusBar;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;
import jtorrent.presentation.viewmodel.TorrentInfoViewModel;

public class TorrentInfoView extends VBox {

    private final ObjectProperty<TorrentInfoViewModel> viewModel = new SimpleObjectProperty<>();

    @FXML
    private DataStatusBar downloadedDataStatusBar;
    @FXML
    private Text downloadedPercentage;
    @FXML
    private DataStatusBar availableDataStatusBar;
    @FXML
    private Text availablePercentage;
    @FXML
    private Text timeElapsed;
    @FXML
    private Text remaining;
    @FXML
    private Text wasted;
    @FXML
    private Text downloaded;
    @FXML
    private Text uploaded;
    @FXML
    private Text seeds;
    @FXML
    private Text downloadSpeed;
    @FXML
    private Text uploadSpeed;
    @FXML
    private Text peers;
    @FXML
    private Text downLimit;
    @FXML
    private Text upLimit;
    @FXML
    private Text shareRatio;
    @FXML
    private Text status;
    @FXML
    private Text saveAs;
    @FXML
    private Text pieces;
    @FXML
    private Text totalSize;
    @FXML
    private Text createdBy;
    @FXML
    private Text createdOn;
    @FXML
    private Text completedOn;
    @FXML
    private Text hash;
    @FXML
    private Text comment;

    public TorrentInfoView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public ObjectProperty<TorrentInfoViewModel> viewModelProperty() {
        return viewModel;
    }

    @FXML
    public void initialize() {

        downloadedDataStatusBar.availabilityProperty().bind(
                viewModel.flatMap(TorrentInfoViewModel::downloadedPiecesProperty).orElse(new BitSet()));
        downloadedDataStatusBar.totalSegmentsProperty().bind(
                viewModel.flatMap(TorrentInfoViewModel::totalPiecesProperty).orElse(0));
        downloadedPercentage.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::downloadedPercentageProperty));

        availableDataStatusBar.availabilityProperty().bind(
                viewModel.flatMap(TorrentInfoViewModel::availablePiecesProperty).orElse(new BitSet()));
        availableDataStatusBar.totalSegmentsProperty().bind(
                viewModel.flatMap(TorrentInfoViewModel::totalPiecesProperty).orElse(0));
        availablePercentage.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::availablePercentageProperty));

        timeElapsed.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::timeElapsedProperty));
        remaining.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::remainingProperty));
        wasted.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::wastedProperty));
        downloaded.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::downloadedProperty));
        uploaded.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::uploadedProperty));
        seeds.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::seedsProperty));
        downloadSpeed.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::downloadSpeedProperty));
        uploadSpeed.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::uploadSpeedProperty));
        peers.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::peersProperty));
        downLimit.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::downLimitProperty));
        upLimit.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::upLimitProperty));
        shareRatio.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::shareRatioProperty));
        status.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::statusProperty));

        saveAs.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::saveAsProperty));
        pieces.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::piecesProperty));
        totalSize.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::totalSizeProperty));
        createdBy.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::createdByProperty));
        createdOn.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::createdOnProperty));
        completedOn.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::completedOnProperty));
        hash.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::hashProperty));
        comment.textProperty().bind(viewModel.flatMap(TorrentInfoViewModel::commentProperty));
    }
}
