package jtorrent.presentation.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;

public class UiTorrentStatus {

    private final StringProperty state;
    private final DoubleProperty progress;

    public UiTorrentStatus(StringProperty state, DoubleProperty progress) {
        this.state = requireNonNull(state);
        this.progress = requireNonNull(progress);
    }

    public ReadOnlyStringProperty stateProperty() {
        return state;
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return progress;
    }
}
