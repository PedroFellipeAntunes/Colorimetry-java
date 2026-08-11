package colorimetry;

/**
 * Descriptor for a distance metric between two color values.
 * Each metric is one class in colorimetry/metric/ that implements
 * this interface.
 *
 * The metric operates on raw value arrays. The quality of the result
 * depends on both the metric and the color space in which the values
 * are expressed.
 */
public interface DistanceMetric {
    /**
     * Canonical display name of this metric.
     *
     * @return display name
     */
    String displayName();

    /**
     * Computes the distance between two raw value arrays of equal length.
     *
     * @param a first color's raw values
     * @param b second color's raw values
     * @return non-negative distance
     */
    double compute(double[] a, double[] b);
}