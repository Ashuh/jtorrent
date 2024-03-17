package jtorrent.presentation.addnewtorrent.viewmodel;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import jtorrent.data.torrent.model.BencodedFile;
import jtorrent.data.torrent.model.BencodedTorrent;
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

    public AddNewTorrentViewModel(BencodedTorrent torrent) {
        saveDirectory.set(Paths.get("download").toAbsolutePath().toString());
        name.set(torrent.getName());
        comment.set(torrent.getComment());
        size.set(DataSize.bestFitBytes(torrent.getTotalSize()).toString());
        LocalDateTime creationDateTime = LocalDateTime.ofEpochSecond(torrent.getCreationDate(), 0,
                OffsetDateTime.now().getOffset());
        date.set(DateFormatter.format(creationDateTime));
        files.set(buildFileTree(torrent.getFiles()));
    }

    private static TreeItem<UiFileInfo> buildFileTree(Collection<BencodedFile> files) {
        Map<String, Long> pathToTotalSize = new HashMap<>();

        for (BencodedFile file : files) {
            String fullPath = "";
            for (String component : file.getPath()) {
                fullPath += "/" + component;
                pathToTotalSize.merge(fullPath, file.getLength(), Long::sum);
            }
        }

        TreeItem<UiFileInfo> root = new TreeItem<>();

        for (BencodedFile file : files) {
            TreeItem<UiFileInfo> current = root;
            String fullPath = "";

            for (String component : file.getPath()) {
                fullPath += "/" + component;
                String size = DataSize.bestFitBytes(pathToTotalSize.get(fullPath)).toString();

                TreeItem<UiFileInfo> currentTemp = current;
                boolean isDirectory = !file.getPath().get(file.getPath().size() - 1).equals(component);

                current = current.getChildren().stream()
                        .filter(item -> item.getValue().getName().equals(component))
                        .findFirst()
                        .orElseGet(() -> {
                            UiFileInfo fileInfo = new UiFileInfo(component, size, isDirectory);
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
