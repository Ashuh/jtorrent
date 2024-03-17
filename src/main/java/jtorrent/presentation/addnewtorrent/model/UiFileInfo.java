package jtorrent.presentation.addnewtorrent.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public record UiFileInfo(StringProperty name, StringProperty size, boolean isDirectory) {

    public UiFileInfo(String name, String size, boolean isDirectory) {
        this(new SimpleStringProperty(name), new SimpleStringProperty(size), isDirectory);
    }
}
