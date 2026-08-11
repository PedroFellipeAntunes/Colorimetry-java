package colorimetry.metric;

import colorimetry.DistanceMetric;

/**
 * Bray-Curtis dissimilarity.
 *
 * Formula: sum(|ai - bi|) / sum(|ai + bi|)
 *
 * Source: Bray, Curtis, "An Ordination of the Upland Forest Communities
 *         of Southern Wisconsin", Ecological Monographs, 1957.
 *
 * Returns a value between 0 (identical) and 1 (maximally dissimilar).
 * Unlike Canberra, normalizes globally across all channels rather than
 * per channel.
 */
public final class BrayCurtis implements DistanceMetric {
    public static final BrayCurtis INSTANCE = new BrayCurtis();

    private BrayCurtis() {}

    @Override
    public String displayName() {
        return "Bray-Curtis";
    }

    @Override
    public double compute(double[] a, double[] b) {
        double num = 0.0;
        double den = 0.0;

        for (int i = 0; i < a.length; i++) {
            num += Math.abs(a[i] - b[i]);
            den += Math.abs(a[i] + b[i]);
        }

        if (den == 0.0) {
            return 0.0;
        }

        return num / den;
    }
}