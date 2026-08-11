package colorimetry.metric;

import colorimetry.DistanceMetric;

/**
 * Euclidean distance (L2 norm).
 *
 * Formula: sqrt(sum((ai - bi)^2))
 *
 * The straight-line distance between two points. The most common
 * distance metric, sensitive to large differences in any single channel.
 */
public final class Euclidean implements DistanceMetric {
    public static final Euclidean INSTANCE = new Euclidean();

    private Euclidean() {}

    @Override
    public String displayName() {
        return "Euclidean";
    }

    @Override
    public double compute(double[] a, double[] b) {
        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }

        return Math.sqrt(sum);
    }
}