package colorimetry.types;

import colorimetry.ColorSpace;

/**
 * Marker interface for linear RGB color spaces.
 *
 * Implemented by all RGB-linear spaces (BT.601, BT.709, BT.2020, Display P3,
 * Adobe RGB, ProPhoto RGB). Used as the parameter type in factory methods
 * like {@code Hsb.of(RgbLike parent)} to enforce compile-time type safety
 * when selecting an RGB parent.
 */
public interface RgbLike extends ColorSpace {
    /**
     * Returns the luma coefficients for this RGB space. These are the Y-row of
     * the RGB-to-XYZ matrix, representing the relative luminance contribution
     * of each primary.
     *
     * @return array of {Wr, Wg, Wb} summing to 1.0
     */
    double[] lumaCoefficients();

    /**
     * Tests whether a CIE XYZ point is representable in this RGB space.
     * Converts to linear RGB via fromParent and checks against component bounds.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return true if all channels are within [componentMin, componentMax]
     */
    @Override
    default boolean isInGamut(double[] xyz) {
        double[] raw = fromParent(xyz);

        for (int i = 0; i < componentCount(); i++) {
            if (raw[i] < componentMin(i) || raw[i] > componentMax(i)) {
                return false;
            }
        }

        return true;
    }
}