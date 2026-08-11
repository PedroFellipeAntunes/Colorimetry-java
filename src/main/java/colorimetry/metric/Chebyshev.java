package colorimetry.metric;

import colorimetry.DistanceMetric;

/**
 * Chebyshev distance (L-infinity norm).
 *
 * Formula: max(|ai - bi|)
 *
 * Reports only the largest difference across all channels. Useful for
 * quality control where no single channel may deviate beyond a threshold.
 */
public final class Chebyshev implements DistanceMetric {
    public static final Chebyshev INSTANCE = new Chebyshev();

    private Chebyshev() {}

    @Override
    public String displayName() {
        return "Chebyshev";
    }

    @Override
    public double compute(double[] a, double[] b) {
        double max = 0.0;

        for (int i = 0; i < a.length; i++) {
            double d = Math.abs(a[i] - b[i]);

            if (d > max) {
                max = d;
            }
        }

        return max;
    }
}