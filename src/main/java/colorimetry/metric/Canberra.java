package colorimetry.metric;

import colorimetry.DistanceMetric;

/**
 * Canberra distance.
 *
 * Formula: sum(|ai - bi| / (|ai| + |bi|))
 *
 * Source: Lance, Williams, "Computer Programs for Hierarchical Polythetic
 *         Classification", The Computer Journal, 1966.
 *
 * A relative distance metric where each channel's contribution is
 * normalized by the magnitudes of both values. Differences near zero
 * weigh more heavily than differences at large values.
 */
public final class Canberra implements DistanceMetric {
    public static final Canberra INSTANCE = new Canberra();

    private Canberra() {}

    @Override
    public String displayName() {
        return "Canberra";
    }

    @Override
    public double compute(double[] a, double[] b) {
        double sum = 0.0;

        for (int i = 0; i < a.length; i++) {
            double denom = Math.abs(a[i]) + Math.abs(b[i]);

            if (denom > 0.0) {
                sum += Math.abs(a[i] - b[i]) / denom;
            }
        }

        return sum;
    }
}