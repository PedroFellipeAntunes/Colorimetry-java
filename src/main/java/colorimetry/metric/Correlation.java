package colorimetry.metric;

import colorimetry.DistanceMetric;

/**
 * Correlation distance.
 *
 * Formula: 1 - Pearson correlation coefficient
 *
 * Similar to Cosine but subtracts the mean of each vector first,
 * measuring whether the patterns of variation are similar regardless
 * of offset. Returns 0 (perfectly correlated) to 2 (anti-correlated).
 */
public final class Correlation implements DistanceMetric {
    public static final Correlation INSTANCE = new Correlation();

    private Correlation() {}

    @Override
    public String displayName() {
        return "Correlation";
    }

    @Override
    public double compute(double[] a, double[] b) {
        double meanA = 0.0;
        double meanB = 0.0;

        for (int i = 0; i < a.length; i++) {
            meanA += a[i];
            meanB += b[i];
        }

        meanA /= a.length;
        meanB /= b.length;

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            double da = a[i] - meanA;
            double db = b[i] - meanB;
            dot += da * db;
            normA += da * da;
            normB += db * db;
        }

        double denom = Math.sqrt(normA) * Math.sqrt(normB);

        if (denom == 0.0) {
            return 0.0;
        }

        return 1.0 - dot / denom;
    }
}