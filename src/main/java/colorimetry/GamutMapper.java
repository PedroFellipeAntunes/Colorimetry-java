package colorimetry;

/**
 * Binary-search gamut mapper.
 * Operates entirely in XYZ D65 space.
 *
 * Given an out-of-gamut XYZ point and a bounded ColorSpace, searches the line
 * segment in XYZ between that point and the space's guaranteed in-gamut neutral.
 * 
 * After ITERATIONS bisections, the result is within 1/2^ITERATIONS of the gamut
 * boundary, at 20 iterations, precision is below 1e-6 of the search range.
 *
 * The approach is space-agnostic: the only coupling to the target space is the
 * isInGamut predicate and the neutralXyz anchor, both provided by ColorSpace.
 */
public final class GamutMapper {
    private static final int ITERATIONS = 20;

    private GamutMapper() {}

    /**
     * Maps an out-of-gamut XYZ point to the nearest in-gamut point by bisecting
     * the line between it and the target space's neutral gray.
     *
     * @param outOfGamutXyz CIE XYZ D65 point outside the target gamut
     * @param target bounded color space that defines the gamut boundary
     * @return CIE XYZ D65 point on or just inside the gamut boundary
     */
    public static double[] map(double[] outOfGamutXyz, ColorSpace target) {
        double[] inside = target.neutralXyz();
        double[] outside = outOfGamutXyz.clone();

        for (int i = 0; i < ITERATIONS; i++) {
            double[] mid = midpoint(inside, outside);
            
            if (target.isInGamut(mid)) {
                inside = mid;
            } else {
                outside = mid;
            }
        }
        
        return inside;
    }

    /**
     * Component-wise midpoint of two XYZ triplets.
     *
     * @param a first XYZ triplet
     * @param b second XYZ triplet
     * @return element-wise average of a and b
     */
    private static double[] midpoint(double[] a, double[] b) {
        return new double[]{
            (a[0] + b[0]) / 2.0,
            (a[1] + b[1]) / 2.0,
            (a[2] + b[2]) / 2.0
        };
    }
}