package jtorrent.domain.util;

import java.util.Arrays;

public class CombinedDoubleSumObservable extends CombinedObservable<Double, Double> {

    @Override
    protected Double combine(Object[] objects) {
        return Arrays.stream(objects)
                .mapToDouble(Double.class::cast)
                .sum();
    }
}
