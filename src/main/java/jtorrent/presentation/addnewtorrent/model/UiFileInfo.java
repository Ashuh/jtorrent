package jtorrent.presentation.addnewtorrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public final class UiFileInfo {

    private final ReadOnlyStringWrapper name;
    private final ReadOnlyStringWrapper size;
    private final boolean isDirectory;

    public UiFileInfo(String name, String size, boolean isDirectory) {
        this(new ReadOnlyStringWrapper(name), new ReadOnlyStringWrapper(size), isDirectory);
    }

    public UiFileInfo(ReadOnlyStringWrapper name, ReadOnlyStringWrapper size, boolean isDirectory) {
        this.name = requireNonNull(name);
        this.size = requireNonNull(size);
        this.isDirectory = isDirectory;
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty sizeProperty() {
        return size.getReadOnlyProperty();
    }

    public boolean isDirectory() {
        return isDirectory;
    }
}
