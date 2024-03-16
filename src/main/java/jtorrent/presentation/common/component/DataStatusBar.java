package jtorrent.presentation.common.component;

import java.util.BitSet;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class DataStatusBar extends Region {

    private static final StyleablePropertyFactory<DataStatusBar> FACTORY =
            new StyleablePropertyFactory<>(Region.getClassCssMetaData());

    private final StyleableProperty<Color> availableColor = FACTORY.createStyleableColorProperty(
            this, "color", "-color-success-fg", s -> s.availableColor, Color.GREEN);
    private final StyleableProperty<Color> unavailableColor = FACTORY.createStyleableColorProperty(
            this, "color", "-color-danger-fg", s -> s.unavailableColor, Color.RED);
    private final IntegerProperty totalSegments = new SimpleIntegerProperty();
    private final ObjectProperty<BitSet> availability = new SimpleObjectProperty<>(new BitSet());
    private final ImageView imageView = new ImageView();
    private WritableImage writableImage;

    public DataStatusBar() {
        setPrefHeight(24);
        imageView.visibleProperty().bind(totalSegments.greaterThan(0));
        widthProperty().addListener((observable, oldValue, newValue) -> redraw());
        totalSegments.addListener((observable, oldValue, newValue) -> redraw());
        availability.addListener((observable, oldValue, newValue) -> updateAvailability(oldValue, newValue));
        availableColorProperty().addListener(observable -> redraw());
        unavailableColorProperty().addListener(observable -> redraw());
        imageView.fitHeightProperty().bind(heightProperty());
        getChildren().add(imageView);
        getStyleClass().add("root");
    }

    /**
     * Calculates the range of X that belong to the given Y, where X and Y are either pixels or segments.
     *
     * @param index        the index of the Y
     * @param itemsPerUnit the number of X per Y
     * @return a BitSet containing the range of X that belong to the given Y
     */
    private static BitSet calculateRange(int index, double itemsPerUnit) {
        BitSet range = new BitSet();
        int firstItem = (int) (index * itemsPerUnit);
        double nextIndexFirstItem = (index + 1) * itemsPerUnit;
        boolean isLastItemAligned = isWholeNumber(nextIndexFirstItem);
        int lastItem = isLastItemAligned ? (int) nextIndexFirstItem - 1 : (int) Math.floor(nextIndexFirstItem);
        range.set(firstItem, lastItem + 1);
        return range;
    }

    private static boolean isWholeNumber(double number) {
        return number % 1 == 0;
    }

    private static BitSet computeChangedBits(BitSet previous, BitSet current) {
        BitSet changed = (BitSet) previous.clone();
        changed.xor(current);
        return changed;
    }

    private Color getColor(double availabilityRatio) {
        return unavailableColor.getValue().interpolate(availableColor.getValue(), availabilityRatio);
    }

    private BitSet pixelToSegments(int pixel) {
        return calculateRange(pixel, totalSegments.get() / getWidth());
    }

    private BitSet segmentToPixels(int segment) {
        return calculateRange(segment, getWidth() / totalSegments.get());
    }

    private void redraw() {
        if (totalSegments.get() <= 0) {
            return;
        }

        int width = (int) getWidth();

        if (width == 0) {
            return;
        }

        WritableImage newImage = new WritableImage(width, 1);
        for (int i = 0; i < width; i++) {
            double availableRatio = calculatePixelAvailabilityRatio(i);
            Color color = getColor(availableRatio);
            newImage.getPixelWriter().setColor(i, 0, color);
        }

        writableImage = newImage;
        imageView.setImage(newImage);
    }

    private void updateAvailability(BitSet previous, BitSet current) {
        BitSet changed = computeChangedBits(previous, current);
        changed.stream()
                .mapToObj(this::segmentToPixels)
                .flatMapToInt(BitSet::stream)
                .distinct()
                .filter(i -> i < writableImage.getWidth())
                .forEach(i -> {
                    double availableRatio = calculatePixelAvailabilityRatio(i);
                    Color color = getColor(availableRatio);
                    writableImage.getPixelWriter().setColor(i, 0, color);
                });
    }

    /**
     * Calculates the percentage of segments within the pixel that are available.
     *
     * @param pixel the pixel for which to calculate the availability ratio
     * @return the availability ratio of the pixel as a double between 0 and 1
     */
    private double calculatePixelAvailabilityRatio(int pixel) {
        BitSet segments = pixelToSegments(pixel);
        BitSet availableSegmentsInPixel = (BitSet) segments.clone();
        availableSegmentsInPixel.and(availability.get());
        int totalSegmentsInPixel = segments.cardinality();
        int availableSegmentsInPixelCount = availableSegmentsInPixel.cardinality();
        return (double) availableSegmentsInPixelCount / totalSegmentsInPixel;
    }

    public IntegerProperty totalSegmentsProperty() {
        return totalSegments;
    }

    public ObjectProperty<BitSet> availabilityProperty() {
        return availability;
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

    @SuppressWarnings("unchecked")
    public ObservableValue<Color> availableColorProperty() {
        return (ObservableValue<Color>) availableColor;
    }

    @SuppressWarnings("unchecked")
    public ObservableValue<Color> unavailableColorProperty() {
        return (ObservableValue<Color>) unavailableColor;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return FACTORY.getCssMetaData();
    }
}
