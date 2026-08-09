package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.xyz.XyzD65;

/**
 * Linear RGB color space with BT.2020 primaries (no gamma).
 *
 * Source: ITU-R BT.2020-2 (2015), ultra-wide gamut for UHDTV.
 *         Primaries: R(0.708, 0.292), G(0.170, 0.797), B(0.131, 0.046).
 *         White point: D65.
 */
public final class Bt2020RgbLinear implements RgbLike {
    public static final Bt2020RgbLinear INSTANCE = new Bt2020RgbLinear();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    private static final double[] NEUTRAL_XYZ = {0.47523, 0.50000, 0.54453};
    private static final double[] LUMA = {0.2627002, 0.6779981, 0.0593017};

    private Bt2020RgbLinear() {}

    @Override
    public String displayName() {
        return "BT.2020 RGB Linear";
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
     * Converts linear [0,1] RGB to CIE XYZ D65 using BT.2020 primaries.
     *
     * @param r red channel in [0, 1]
     * @param g green channel in [0, 1]
     * @param b blue channel in [0, 1]
     * @return CIE XYZ D65 triplet
     */
    static double[] linearToXyz(double r, double g, double b) {
        return new double[] {
            0.6369580 * r + 0.1446169 * g + 0.1688810 * b,
            LUMA[0] * r + LUMA[1] * g + LUMA[2] * b,
            0.0000000 * r + 0.0280727 * g + 1.0609851 * b
        };
    }

    /**
     * Converts CIE XYZ D65 to linear [0,1] RGB using inverse BT.2020 matrix.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return linear RGB in [0, 1] (may exceed bounds for out-of-gamut colors)
     */
    static double[] xyzToLinear(double[] xyz) {
        return new double[] {
             1.7166512 * xyz[0] - 0.3556708 * xyz[1] - 0.2533663 * xyz[2],
            -0.6666844 * xyz[0] + 1.6164812 * xyz[1] + 0.0157685 * xyz[2],
             0.0176399 * xyz[0] - 0.0427706 * xyz[1] + 0.9421031 * xyz[2]
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