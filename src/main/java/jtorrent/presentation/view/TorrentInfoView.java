package jtorrent.presentation.view;

import java.io.IOException;
import java.util.BitSet;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import jtorrent.presentation.model.UiTorrentInfo;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;
import jtorrent.presentation.viewmodel.ViewModel;

public class TorrentInfoView extends VBox {

    private final ObjectProperty<ViewModel> viewModel = new SimpleObjectProperty<>();

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

    public ObjectProperty<ViewModel> viewModelProperty() {
        return viewModel;
    }

    @FXML
    public void initialize() {
        var torrentInfo = viewModel.flatMap(ViewModel::getTorrentInfo);

        downloadedDataStatusBar.availabilityProperty().bind(
                torrentInfo
                        .flatMap(UiTorrentInfo::downloadedPiecesProperty)
                        .orElse(new BitSet())
        );
        downloadedDataStatusBar.totalSegmentsProperty().bind(
                torrentInfo
                        .flatMap(UiTorrentInfo::totalPiecesProperty)
                        .orElse(0)
        );
        downloadedPercentage.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::downloadedPercentageProperty));

        availableDataStatusBar.availabilityProperty().bind(
                torrentInfo
                        .flatMap(UiTorrentInfo::availablePiecesProperty)
                        .orElse(new BitSet())
        );
        availableDataStatusBar.totalSegmentsProperty().bind(
                torrentInfo
                        .flatMap(UiTorrentInfo::totalPiecesProperty)
                        .orElse(0)
        );
        availablePercentage.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::availablePercentageProperty));

        timeElapsed.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::timeElapsedProperty));
        remaining.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::remainingProperty));
        wasted.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::wastedProperty));
        downloaded.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::downloadedProperty));
        uploaded.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::uploadedProperty));
        seeds.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::seedsProperty));
        downloadSpeed.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::downloadSpeedProperty));
        uploadSpeed.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::uploadSpeedProperty));
        peers.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::peersProperty));
        downLimit.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::downLimitProperty));
        upLimit.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::upLimitProperty));
        shareRatio.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::shareRatioProperty));
        status.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::statusProperty));

        saveAs.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::saveAsProperty));
        pieces.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::piecesProperty));
        totalSize.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::totalSizeProperty));
        createdBy.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::createdByProperty));
        createdOn.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::createdOnProperty));
        completedOn.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::completedOnProperty));
        hash.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::hashProperty));
        comment.textProperty().bind(torrentInfo.flatMap(UiTorrentInfo::commentProperty));
    }
}
