package jtorrent.torrent.presentation.view;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class DataStatusBar extends Pane {

    /**
     * The actual width of each segment.
     */
    private final DoubleBinding segmentWidthBinding;
    /**
     * The width used to display the segment. This is the actual width plus a small amount.
     * This is used to avoid gaps between segments.
     */
    private final DoubleBinding segmentDisplayWidthBinding;
    private final IntegerProperty totalSegments = new SimpleIntegerProperty();
    private final ObjectProperty<BitSet> availability = new SimpleObjectProperty<>(new BitSet());
    private final ObjectProperty<Color> availableColor = new SimpleObjectProperty<>(Color.GREEN);
    private final ObjectProperty<Color> unavailableColor = new SimpleObjectProperty<>(Color.RED);
    private final Map<Integer, Rectangle> indexToRectangle = new HashMap<>();

    public DataStatusBar() {
        segmentWidthBinding = widthProperty().divide(totalSegments);
        segmentDisplayWidthBinding = segmentWidthBinding.multiply(1.5);
        availability.addListener((observable, oldValue, newValue) -> updateAvailability(oldValue, newValue));
        backgroundProperty().bind(unavailableColor.map(Background::fill));
        setPrefHeight(24);
    }

    private void updateAvailability(BitSet previous, BitSet current) {
        BitSet changed = (BitSet) previous.clone();
        changed.xor(current);
        changed.stream()
                .filter(i -> i < totalSegments.get())
                .forEach(this::flipColor);
    }

    private void flipColor(int index) {
        if (indexToRectangle.containsKey(index)) {
            Rectangle rectangle = indexToRectangle.remove(index);
            getChildren().remove(rectangle);
        } else {
            Rectangle rectangle = new Rectangle();
            rectangle.xProperty().bind(segmentWidthBinding.multiply(index));
            rectangle.widthProperty().bind(segmentDisplayWidthBinding);
            rectangle.heightProperty().bind(heightProperty());
            rectangle.fillProperty().bind(availableColorProperty());
            rectangle.setStrokeWidth(0);
            getChildren().add(rectangle);
            indexToRectangle.put(index, rectangle);
        }
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

    public IntegerProperty totalSegmentsProperty() {
        return totalSegments;
    }

    public BitSet getAvailability() {
        return availability.get();
    }

    public void setAvailability(BitSet availability) {
        this.availability.set(availability);
    }

    public ObjectProperty<BitSet> availabilityProperty() {
        return availability;
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
