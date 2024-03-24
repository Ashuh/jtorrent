package jtorrent.presentation.addnewtorrent.viewmodel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import jtorrent.domain.torrent.model.FileMetadata;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.presentation.addnewtorrent.model.UiFileInfo;
import jtorrent.presentation.common.util.DataSize;
import jtorrent.presentation.common.util.DateFormatter;

public class AddNewTorrentViewModel {

    private final StringProperty saveDirectory = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final ReadOnlyStringWrapper comment = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper size = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper date = new ReadOnlyStringWrapper();
    private final ReadOnlyObjectWrapper<TreeItem<UiFileInfo>> files = new ReadOnlyObjectWrapper<>();

    public AddNewTorrentViewModel(TorrentMetadata torrent) {
        saveDirectory.set(Paths.get("download").toAbsolutePath().toString());
        name.set(torrent.fileInfo().getName());
        comment.set(torrent.comment());
        size.set(DataSize.bestFitBytes(torrent.fileInfo().getTotalFileSize()).toString());
        LocalDateTime creationDateTime = torrent.creationDate();
        date.set(DateFormatter.format(creationDateTime));
        files.set(buildFileTree(torrent.fileInfo().getFileMetaData()));
    }

    private static TreeItem<UiFileInfo> buildFileTree(Collection<FileMetadata> files) {
        Map<Path, Long> pathToTotalSize = new HashMap<>();

        for (FileMetadata file : files) {
            Path fullPath = Path.of("");

            for (Path component : file.path()) {
                fullPath = fullPath.resolve(component);
                pathToTotalSize.merge(fullPath, file.size(), Long::sum);
            }
        }

        TreeItem<UiFileInfo> root = new TreeItem<>();

        for (FileMetadata file : files) {
            TreeItem<UiFileInfo> current = root;
            Path fullPath = Path.of("");

            for (Path component : file.path()) {
                fullPath = fullPath.resolve(component);
                String size = DataSize.bestFitBytes(pathToTotalSize.get(fullPath)).toString();
                TreeItem<UiFileInfo> currentTemp = current;
                boolean isDirectory = !file.path().equals(component);
                String componentName = component.toString();

                current = current.getChildren().stream()
                        .filter(item -> item.getValue().getName().equals(componentName))
                        .findFirst()
                        .orElseGet(() -> {
                            UiFileInfo fileInfo = new UiFileInfo(componentName, size, isDirectory);
                            TreeItem<UiFileInfo> item = new TreeItem<>(fileInfo);
                            currentTemp.getChildren().add(item);
                            return item;
                        });
            }
        }
        return root;
    }

    public String getSaveDirectory() {
        return saveDirectory.get();
    }

    public StringProperty saveDirectoryProperty() {
        return saveDirectory;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ReadOnlyStringProperty commentProperty() {
        return comment.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty sizeProperty() {
        return size.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty dateProperty() {
        return date.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<TreeItem<UiFileInfo>> getFiles() {
        return files.getReadOnlyProperty();
    }
}
