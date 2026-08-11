package colorimetry.metric;

import colorimetry.DistanceMetric;

/**
 * Cosine distance.
 *
 * Formula: 1 - (a . b) / (|a| * |b|)
 *
 * Measures difference in direction, ignoring magnitude. Two colors
 * with the same channel proportions but different brightness have
 * distance zero. Returns 0 (identical direction) to 2 (opposite).
 */
public final class Cosine implements DistanceMetric {
    public static final Cosine INSTANCE = new Cosine();

    private Cosine() {}

    @Override
    public String displayName() {
        return "Cosine";
    }

    @Override
    public double compute(double[] a, double[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denom = Math.sqrt(normA) * Math.sqrt(normB);

        if (denom == 0.0) {
            return 0.0;
        }

        return 1.0 - dot / denom;
    }
}