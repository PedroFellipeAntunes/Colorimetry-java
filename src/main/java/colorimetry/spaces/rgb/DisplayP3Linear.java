package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.xyz.XyzD65;

/**
 * Linear RGB color space with Display P3 (DCI-P3 D65) primaries (no gamma).
 *
 * Source: DCI-P3 primaries adapted to D65 white point, as used by Apple
 *         Display P3 and CSS color-gamut: p3.
 *         Primaries: R(0.680, 0.320), G(0.265, 0.690), B(0.150, 0.060).
 *         White point: D65.
 */
public final class DisplayP3Linear implements RgbLike {
    public static final DisplayP3Linear INSTANCE = new DisplayP3Linear();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    private static final double[] NEUTRAL_XYZ = {0.47523, 0.50000, 0.54453};
    private static final double[] LUMA = {0.2289745641, 0.6917385218, 0.0792869141};

    private DisplayP3Linear() {}

    @Override
    public String displayName() {
        return "Display P3 Linear";
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
     * Converts linear [0,1] RGB to CIE XYZ D65 using Display P3 primaries.
     *
     * @param r red channel in [0, 1]
     * @param g green channel in [0, 1]
     * @param b blue channel in [0, 1]
     * @return CIE XYZ D65 triplet
     */
    static double[] linearToXyz(double r, double g, double b) {
        return new double[] {
            0.4865709486 * r + 0.2656676932 * g + 0.1982172852 * b,
            LUMA[0] * r + LUMA[1] * g + LUMA[2] * b,
            0.0000000000 * r + 0.0451133819 * g + 1.0439443689 * b
        };
    }

    /**
     * Converts CIE XYZ D65 to linear [0,1] RGB using inverse Display P3 matrix.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return linear RGB in [0, 1] (may exceed bounds for out-of-gamut colors)
     */
    static double[] xyzToLinear(double[] xyz) {
        return new double[] {
             2.4934969119 * xyz[0] - 0.9313836179 * xyz[1] - 0.4027107845 * xyz[2],
            -0.8294889696 * xyz[0] + 1.7626640603 * xyz[1] + 0.0236246858 * xyz[2],
             0.0358458302 * xyz[0] - 0.0761723893 * xyz[1] + 0.9568845240 * xyz[2]
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