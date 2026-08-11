package colorimetry.registry;

import colorimetry.DistanceMetric;
import colorimetry.metric.*;

/**
 * Central registry for distance metrics.
 * Pre-populated with all built-in metrics in the static initializer.
 * Users can add custom metrics via {@link Registry#register(Object)}.
 */
public final class MetricRegistry {
    public static final Registry<DistanceMetric> INSTANCE = new Registry<>("DistanceMetric", DistanceMetric::displayName);

    static {
        INSTANCE.register(Euclidean.INSTANCE);
        INSTANCE.register(SquaredEuclidean.INSTANCE);
        INSTANCE.register(Manhattan.INSTANCE);
        INSTANCE.register(Chebyshev.INSTANCE);
        INSTANCE.register(Canberra.INSTANCE);
        INSTANCE.register(BrayCurtis.INSTANCE);
        INSTANCE.register(Cosine.INSTANCE);
        INSTANCE.register(Correlation.INSTANCE);
    }

    private MetricRegistry() {}
}