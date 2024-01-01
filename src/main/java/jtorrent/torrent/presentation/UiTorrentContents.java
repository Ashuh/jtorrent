package jtorrent.torrent.presentation;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import jtorrent.common.presentation.util.DataUnitFormatter;
import jtorrent.common.presentation.util.DateFormatter;
import jtorrent.torrent.domain.model.File;
import jtorrent.torrent.domain.model.Torrent;

public class UiTorrentContents {

    private final StringProperty saveDirectory;
    private final StringProperty name;
    private final StringProperty comment;
    private final StringProperty size;
    private final StringProperty date;
    private final TreeItem<FileInfo> files;

    public UiTorrentContents(StringProperty saveDirectory, StringProperty name, StringProperty comment,
            StringProperty size, StringProperty date, TreeItem<FileInfo> files) {
        this.saveDirectory = saveDirectory;
        this.name = name;
        this.comment = comment;
        this.size = size;
        this.date = date;
        this.files = files;
    }

    public static UiTorrentContents forTorrent(Torrent torrent) {
        StringProperty saveDirectory = new SimpleStringProperty(torrent.getSaveDirectory().toString());
        StringProperty name = new SimpleStringProperty(torrent.getName());
        StringProperty comment = new SimpleStringProperty(torrent.getComment());
        StringProperty size = new SimpleStringProperty(DataUnitFormatter.formatSize(torrent.getTotalSize()));
        StringProperty date = new SimpleStringProperty(DateFormatter.format(torrent.getCreationDate()));
        TreeItem<FileInfo> files = buildFileTree(torrent.getFiles());
        return new UiTorrentContents(saveDirectory, name, comment, size, date, files);
    }

    private static TreeItem<FileInfo> buildFileTree(Collection<File> files) {
        Map<TreeItem<FileInfo>, Long> treeItemToTotalSize = new HashMap<>();
        TreeItem<FileInfo> root = new TreeItem<>();

        for (File file : files) {
            TreeItem<FileInfo> current = root;

            for (Path component : file.getPath()) {
                TreeItem<FileInfo> currentTemp = current;
                String componentName = component.getFileName().toString();
                boolean isDirectory = !file.getPath().getFileName().equals(component);

                current = current.getChildren().stream()
                        .filter(item -> item.getValue().name().get().equals(componentName))
                        .findFirst()
                        .orElseGet(() -> {
                            FileInfo fileInfo = new FileInfo(componentName, "", isDirectory);
                            TreeItem<FileInfo> item = new TreeItem<>(fileInfo);
                            currentTemp.getChildren().add(item);
                            return item;
                        });

                long totalSize = treeItemToTotalSize.computeIfAbsent(current, key -> 0L) + file.getSize();
                treeItemToTotalSize.put(current, totalSize);
            }

            treeItemToTotalSize.forEach((item, totalSize) -> {
                String size = DataUnitFormatter.formatSize(totalSize);
                item.getValue().size().set(size);
            });
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

    public String getComment() {
        return comment.get();
    }

    public StringProperty commentProperty() {
        return comment;
    }

    public String getSize() {
        return size.get();
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public String getDate() {
        return date.get();
    }

    public StringProperty dateProperty() {
        return date;
    }

    public TreeItem<FileInfo> getFiles() {
        return files;
    }

    public record FileInfo(StringProperty name, StringProperty size, boolean isDirectory) {

        public FileInfo(String name, String size, boolean isDirectory) {
            this(new SimpleStringProperty(name), new SimpleStringProperty(size), isDirectory);
        }
    }
}
