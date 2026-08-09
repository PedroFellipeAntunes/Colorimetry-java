package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.xyz.XyzD65;

/**
 * Linear RGB color space with BT.601 (SMPTE C) primaries (no gamma).
 *
 * Source: ITU-R BT.601-7 (2011), SMPTE C primaries (525-line variant).
 *         Primaries: R(0.630, 0.340), G(0.310, 0.595), B(0.155, 0.070).
 *         White point: D65.
 */
public final class Bt601RgbLinear implements RgbLike {
    public static final Bt601RgbLinear INSTANCE = new Bt601RgbLinear();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    private static final double[] NEUTRAL_XYZ = {0.47524, 0.50000, 0.49441};
    private static final double[] LUMA = {0.2124132, 0.7010437, 0.0865432};

    private Bt601RgbLinear() {}

    @Override
    public String displayName() {
        return "BT.601 RGB Linear";
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
     * Converts linear [0,1] RGB to CIE XYZ D65 using SMPTE C primaries.
     *
     * @param r red channel in [0, 1]
     * @param g green channel in [0, 1]
     * @param b blue channel in [0, 1]
     * @return CIE XYZ D65 triplet
     */
    static double[] linearToXyz(double r, double g, double b) {
        return new double[] {
            0.3935891 * r + 0.3652497 * g + 0.1916313 * b,
            LUMA[0] * r + LUMA[1] * g + LUMA[2] * b,
            0.0187423 * r + 0.1119313 * g + 0.9581563 * b
        };
    }

    /**
     * Converts CIE XYZ D65 to linear [0,1] RGB using inverse SMPTE C matrix.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return linear RGB in [0, 1] (may exceed bounds for out-of-gamut colors)
     */
    static double[] xyzToLinear(double[] xyz) {
        return new double[] {
             3.5053960 * xyz[0] - 1.7394894 * xyz[1] - 0.5439640 * xyz[2],
            -1.0690722 * xyz[0] + 1.9778245 * xyz[1] + 0.0351722 * xyz[2],
             0.0563200 * xyz[0] - 0.1970226 * xyz[1] + 1.0502026 * xyz[2]
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