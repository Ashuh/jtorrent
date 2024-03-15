package jtorrent.presentation.viewmodel;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jtorrent.domain.Client;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.presentation.model.UiFileInfo;

public class FileInfoViewModel {

    private final Client client;
    private final ObservableList<UiFileInfo> fileInfos = FXCollections.observableArrayList();

    public FileInfoViewModel(Client client) {
        this.client = requireNonNull(client);
    }

    public void setSelectedTorrent(Torrent torrent) {
        fileInfos.forEach(UiFileInfo::dispose);

        if (torrent == null) {
            fileInfos.clear();
            return;
        }

        List<UiFileInfo> selectedUiFilesInfos = torrent.getFilesWithInfo().stream()
                .map(UiFileInfo::fromDomain)
                .toList();
        Platform.runLater(() -> fileInfos.setAll(selectedUiFilesInfos));
    }

    public ObservableList<UiFileInfo> getFileInfos() {
        return fileInfos;
    }
}