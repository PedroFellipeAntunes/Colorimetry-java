package colorimetry;

/**
 * A grayscale conversion method. Each implementation converts a color (given as CIE XYZ D65)
 * to its achromatic equivalent using a specific luminance/brightness model.
 *
 * The result is always a CIE XYZ D65 triplet representing a neutral gray.
 * The caller converts to/from XYZ using the existing ColorSpace pipeline.
 *
 * Usage:
 *   ColorValue gray = myColor.toGrayscale(Bt709Luma.INSTANCE);
 */
public interface Grayscale {
    /**
     * Converts a color to its achromatic equivalent.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return CIE XYZ D65 triplet representing the corresponding gray
     */
    double[] toGrayXyz(double[] xyz);

    /**
     * Display name of this grayscale method (e.g. "BT.709 Luma").
     *
     * @return display name
     */
    String displayName();
}