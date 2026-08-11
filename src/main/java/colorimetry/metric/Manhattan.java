package colorimetry.metric;

import colorimetry.DistanceMetric;

/**
 * Manhattan distance (L1 norm, city block distance).
 *
 * Formula: sum(|ai - bi|)
 *
 * Sum of absolute differences per channel. Less sensitive to outliers
 * in a single channel compared to Euclidean.
 */
public final class Manhattan implements DistanceMetric {
    public static final Manhattan INSTANCE = new Manhattan();

    private Manhattan() {}

    @Override
    public String displayName() {
        return "Manhattan";
    }

    @Override
    public double compute(double[] a, double[] b) {
        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }

        return sum;
    }
}