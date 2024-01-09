package jtorrent.presentation.view;

import java.net.URL;
import java.util.BitSet;
import java.util.ResourceBundle;
import java.util.function.Function;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;
import jtorrent.presentation.model.UiTorrentInfo;

public class TorrentInfoView implements Initializable {

    private final ObjectProperty<UiTorrentInfo> torrentInfo = new SimpleObjectProperty<>();

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        bind(downloadedDataStatusBar.availabilityProperty(), UiTorrentInfo::downloadedPiecesProperty, new BitSet());
        bind(downloadedDataStatusBar.totalSegmentsProperty(), UiTorrentInfo::totalPiecesProperty, 0);
        bindText(downloadedPercentage, UiTorrentInfo::downloadedPercentageProperty, "");

        bind(availableDataStatusBar.availabilityProperty(), UiTorrentInfo::availablePiecesProperty, new BitSet());
        bind(availableDataStatusBar.totalSegmentsProperty(), UiTorrentInfo::totalPiecesProperty, 0);
        bindText(availablePercentage, UiTorrentInfo::availablePercentageProperty, "");

        bindText(timeElapsed, UiTorrentInfo::timeElapsedProperty, "");
        bindText(remaining, UiTorrentInfo::remainingProperty, "");
        bindText(wasted, UiTorrentInfo::wastedProperty, "");
        bindText(downloaded, UiTorrentInfo::downloadedProperty, "");
        bindText(uploaded, UiTorrentInfo::uploadedProperty, "");
        bindText(seeds, UiTorrentInfo::seedsProperty, "");
        bindText(downloadSpeed, UiTorrentInfo::downloadSpeedProperty, "");
        bindText(uploadSpeed, UiTorrentInfo::uploadSpeedProperty, "");
        bindText(peers, UiTorrentInfo::peersProperty, "");
        bindText(downLimit, UiTorrentInfo::downLimitProperty, "");
        bindText(upLimit, UiTorrentInfo::upLimitProperty, "");
        bindText(shareRatio, UiTorrentInfo::shareRatioProperty, "");
        bindText(status, UiTorrentInfo::statusProperty, "");

        bindText(saveAs, UiTorrentInfo::saveAsProperty, "");
        bindText(pieces, UiTorrentInfo::piecesProperty, "");
        bindText(totalSize, UiTorrentInfo::totalSizeProperty, "");
        bindText(createdBy, UiTorrentInfo::createdByProperty, "");
        bindText(createdOn, UiTorrentInfo::createdOnProperty, "");
        bindText(completedOn, UiTorrentInfo::completedOnProperty, "");
        bindText(hash, UiTorrentInfo::hashProperty, "");
        bindText(comment, UiTorrentInfo::commentProperty, "");
    }

    private void bindText(Text text, Function<UiTorrentInfo, Property<String>> mapper, String defaultValue) {
        bind(text.textProperty(), mapper, defaultValue);
    }

    private <T> void bind(Property<T> property, Function<UiTorrentInfo, Property<T>> mapper, T defaultValue) {
        property.bind(torrentInfo.flatMap(mapper).orElse(defaultValue));
    }

    public UiTorrentInfo getTorrentInfo() {
        return torrentInfo.get();
    }

    public ObjectProperty<UiTorrentInfo> torrentInfoProperty() {
        return torrentInfo;
    }
}