package jtorrent.presentation.view;

import java.util.BitSet;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class DataStatusBar extends Pane {

    private final IntegerProperty totalSegments = new SimpleIntegerProperty();
    private final ObjectProperty<BitSet> availability = new SimpleObjectProperty<>(new BitSet());
    private final ObjectProperty<Color> availableColor = new SimpleObjectProperty<>(Color.GREEN);
    private final ObjectProperty<Color> unavailableColor = new SimpleObjectProperty<>(Color.RED);
    private final ImageView imageView = new ImageView();
    private WritableImage writableImage = new WritableImage(1, 1);

    public DataStatusBar() {
        setPrefHeight(24);
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());
        getChildren().add(imageView);
        totalSegments.addListener((observable, oldValue, newValue) -> updateNumSegments(newValue.intValue()));
        availability.addListener((observable, oldValue, newValue) -> updateAvailability(oldValue, newValue));
    }

    private void updateNumSegments(int numSegments) {
        WritableImage newImage = new WritableImage(numSegments, 1);
        for (int i = 0; i < numSegments; i++) {
            Color color = availability.get().get(i) ? Color.GREEN : Color.RED;
            newImage.getPixelWriter().setColor(i, 0, color);
        }

        writableImage = newImage;
        imageView.setImage(newImage);
    }

    private void updateAvailability(BitSet previous, BitSet current) {
        BitSet changed = computeChangedBits(previous, current);
        changed.stream()
                .filter(i -> i < writableImage.getWidth())
                .forEach(i -> {
                    Color color = availability.get().get(i) ? Color.GREEN : Color.RED;
                    writableImage.getPixelWriter().setColor(i, 0, color);
                });
    }

    private static BitSet computeChangedBits(BitSet previous, BitSet current) {
        BitSet changed = (BitSet) previous.clone();
        changed.xor(current);
        return changed;
    }

    public IntegerProperty totalSegmentsProperty() {
        return totalSegments;
    }

    public ObjectProperty<BitSet> availabilityProperty() {
        return availability;
    }

    public ObjectProperty<Color> availableColorProperty() {
        return availableColor;
    }

    public int getTotalSegments() {
        return totalSegments.get();
    }

    public void setTotalSegments(int totalSegments) {
        this.totalSegments.set(totalSegments);
    }

    public BitSet getAvailability() {
        return availability.get();
    }

    public void setAvailability(BitSet availability) {
        this.availability.set(availability);
    }

    public Color getAvailableColor() {
        return availableColor.get();
    }

    public void setAvailableColor(Color availableColor) {
        this.availableColor.set(availableColor);
    }

    public Color getUnavailableColor() {
        return unavailableColor.get();
    }

    public void setUnavailableColor(Color unavailableColor) {
        this.unavailableColor.set(unavailableColor);
    }

    public ObjectProperty<Color> unavailableColorProperty() {
        return unavailableColor;
    }
}
