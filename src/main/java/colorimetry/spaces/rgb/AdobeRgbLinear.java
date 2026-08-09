package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.xyz.XyzD65;

/**
 * Linear RGB color space with Adobe RGB (1998) primaries (no gamma).
 *
 * Source: Adobe Systems, "Adobe RGB (1998) Color Image Encoding", 2005.
 *         Primaries: R(0.64, 0.33), G(0.21, 0.71), B(0.15, 0.06).
 *         White point: D65.
 */
public final class AdobeRgbLinear implements RgbLike {
    public static final AdobeRgbLinear INSTANCE = new AdobeRgbLinear();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    private static final double[] NEUTRAL_XYZ = {0.47524, 0.50000, 0.54442};
    private static final double[] LUMA = {0.2973769, 0.6273491, 0.0752741};

    private AdobeRgbLinear() {}

    @Override
    public String displayName() {
        return "Adobe RGB Linear";
    }

    @Override
    public int componentCount() {
        return NAMES.length;
    }

    @Override
    public double componentMin(int i) {
        return 0.0;
    }

    @Override
    public double componentMax(int i) {
        return 255.0;
    }

    @Override
    public double componentDefault(int i) {
        return 0.0;
    }

    @Override
    public boolean isBounded() {
        return true;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return XyzD65.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        return linearToXyz(raw[0] / 255.0, raw[1] / 255.0, raw[2] / 255.0);
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double[] linear = xyzToLinear(parentRaw);

        return new double[] {
            linear[0] * 255.0,
            linear[1] * 255.0,
            linear[2] * 255.0
        };
    }

    // ===== MATH =====

    /**
     * Converts linear [0,1] RGB to CIE XYZ D65 using Adobe RGB primaries.
     *
     * @param r red channel in [0, 1]
     * @param g green channel in [0, 1]
     * @param b blue channel in [0, 1]
     * @return CIE XYZ D65 triplet
     */
    static double[] linearToXyz(double r, double g, double b) {
        return new double[] {
            0.5767309 * r + 0.1855540 * g + 0.1881852 * b,
            LUMA[0] * r + LUMA[1] * g + LUMA[2] * b,
            0.0270343 * r + 0.0706872 * g + 0.9911085 * b
        };
    }

    /**
     * Converts CIE XYZ D65 to linear [0,1] RGB using inverse Adobe RGB matrix.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return linear RGB in [0, 1] (may exceed bounds for out-of-gamut colors)
     */
    static double[] xyzToLinear(double[] xyz) {
        return new double[] {
             2.0415879 * xyz[0] - 0.5650070 * xyz[1] - 0.3447314 * xyz[2],
            -0.9692660 * xyz[0] + 1.8760108 * xyz[1] + 0.0415560 * xyz[2],
             0.0134474 * xyz[0] - 0.1183897 * xyz[1] + 1.0154096 * xyz[2]
        };
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double formatRaw(double value) {
        return Math.round(value);
    }

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {raw[0] / 255.0, raw[1] / 255.0, raw[2] / 255.0};
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * 255.0,
            normalized[1] * 255.0,
            normalized[2] * 255.0
        };
    }

    @Override
    public double[] lumaCoefficients() {
        return LUMA.clone();
    }

    @Override
    public double[] neutralXyz() {
        return NEUTRAL_XYZ.clone();
    }
}