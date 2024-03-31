package jtorrent.presentation.common.util;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

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
