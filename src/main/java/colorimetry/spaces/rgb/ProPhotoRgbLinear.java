package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.xyz.XyzD50;

/**
 * Linear RGB color space with ProPhoto (ROMM RGB) primaries (no gamma).
 *
 * Source: Kodak, "ROMM RGB Color Encoding Specification", 1999.
 *         Primaries: R(0.7347, 0.2653), G(0.1596, 0.8404), B(0.0366, 0.0001).
 *         White point: D50.
 *
 * Note: parent is XyzD50 (not XyzD65) because ProPhoto is defined under
 * D50 illuminant. Chromatic adaptation to D65 happens via Bradford in the
 * XyzD50 → Xyz → XyzD65 path.
 */
public final class ProPhotoRgbLinear implements RgbLike {
    public static final ProPhotoRgbLinear INSTANCE = new ProPhotoRgbLinear();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    // D50 white point at 50% intensity
    private static final double[] NEUTRAL_XYZ = {0.48211, 0.50000, 0.41261};
    private static final double[] LUMA = {0.2880711282, 0.7118432178, 0.0000856540};

    private ProPhotoRgbLinear() {}

    @Override
    public String displayName() {
        return "ProPhoto RGB Linear";
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
        return XyzD50.INSTANCE;
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
     * Converts linear [0,1] RGB to CIE XYZ D50 using ProPhoto primaries.
     *
     * @param r red channel in [0, 1]
     * @param g green channel in [0, 1]
     * @param b blue channel in [0, 1]
     * @return CIE XYZ D50 triplet
     */
    static double[] linearToXyz(double r, double g, double b) {
        return new double[] {
            0.7977604896 * r + 0.1351917082 * g + 0.0313534211 * b,
            LUMA[0] * r + LUMA[1] * g + LUMA[2] * b,
            0.0000000000 * r + 0.0000000000 * g + 0.8251046026 * b
        };
    }

    /**
     * Converts CIE XYZ D50 to linear [0,1] RGB using inverse ProPhoto matrix.
     *
     * @param xyz CIE XYZ D50 triplet
     * @return linear RGB in [0, 1] (may exceed bounds for out-of-gamut colors)
     */
    static double[] xyzToLinear(double[] xyz) {
        return new double[] {
             1.3459433009 * xyz[0] - 0.2556075298 * xyz[1] - 0.0511118023 * xyz[2],
            -0.5445989113 * xyz[0] + 1.5081673429 * xyz[1] + 0.0205351076 * xyz[2],
             0.0000000000 * xyz[0] + 0.0000000000 * xyz[1] + 1.2118127757 * xyz[2]
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