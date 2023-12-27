package jtorrent.torrent.presentation;

import io.reactivex.rxjava3.functions.BiFunction;

class CalculateEtaCombiner implements BiFunction<Long, Double, Double> {

    private final long size;

    public CalculateEtaCombiner(long size) {
        this.size = size;
    }

    @Override
    public Double apply(Long downloaded, Double rate) {
        if (downloaded == size) {
            return 0.0;
        }

        if (rate == 0) {
            return Double.POSITIVE_INFINITY;
        } else {
            return (size - downloaded) / rate;
        }
    }
}
