package jtorrent.common.presentation.util;

import static jtorrent.common.domain.util.ValidationUtil.requireNonNull;

import io.reactivex.rxjava3.functions.Consumer;
import javafx.application.Platform;
import javafx.beans.property.Property;

public class UpdatePropertyConsumer<T> implements Consumer<T> {

    private final Property<T> property;

    public UpdatePropertyConsumer(Property<T> property) {
        this.property = requireNonNull(property);
    }

    @Override
    public void accept(T t) {
        Platform.runLater(() -> property.setValue(t));
    }
}
