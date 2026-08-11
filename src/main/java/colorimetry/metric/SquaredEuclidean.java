package colorimetry.metric;

import colorimetry.DistanceMetric;

/**
 * Squared Euclidean distance.
 *
 * Formula: sum((ai - bi)^2)
 *
 * Same as Euclidean but without the square root. Faster when only
 * relative ordering matters (which color is closer), not the absolute
 * distance value.
 */
public final class SquaredEuclidean implements DistanceMetric {
    public static final SquaredEuclidean INSTANCE = new SquaredEuclidean();

    private SquaredEuclidean() {}

    @Override
    public String displayName() {
        return "Squared Euclidean";
    }

    @Override
    public double compute(double[] a, double[] b) {
        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }

        return sum;
    }
}