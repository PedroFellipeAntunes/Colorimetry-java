package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.xyz.XyzD65;

/**
 * Linear RGB color space with BT.709/sRGB primaries (no gamma).
 *
 * Source: ITU-R BT.709-6 (2015), same primaries as IEC 61966-2-1 sRGB.
 *         Primaries: R(0.64, 0.33), G(0.30, 0.60), B(0.15, 0.06).
 *         White point: D65.
 */
public final class Bt709RgbLinear implements RgbLike {
    public static final Bt709RgbLinear INSTANCE = new Bt709RgbLinear();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    // D65 white point at 50% linear intensity, used as gamut mapping anchor
    private static final double[] NEUTRAL_XYZ = {0.47524, 0.50000, 0.54442};
    // Y-row of RGB→XYZ matrix: relative luminance contribution of each primary
    private static final double[] LUMA = {0.2126729, 0.7151522, 0.0721750};

    private Bt709RgbLinear() {}

    @Override
    public String displayName() {
        return "BT.709 RGB Linear";
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
        // Rgb raw [0,255] → linear [0,1] → XYZ D65
        return linearToXyz(raw[0] / 255.0, raw[1] / 255.0, raw[2] / 255.0);
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // XYZ D65 → linear [0,1] → Rgb raw [0,255] (rounded)
        double[] linear = xyzToLinear(parentRaw);

        return new double[]{
            Math.round(linear[0] * 255.0),
            Math.round(linear[1] * 255.0),
            Math.round(linear[2] * 255.0)
        };
    }

    // ===== MATH =====

    /**
     * Converts linear [0,1] RGB to CIE XYZ D65 using the BT.709 primaries matrix.
     *
     * @param r red channel in [0, 1]
     * @param g green channel in [0, 1]
     * @param b blue channel in [0, 1]
     * @return CIE XYZ D65 triplet
     */
    static double[] linearToXyz(double r, double g, double b) {
        return new double[]{
            0.4124564 * r + 0.3575761 * g + 0.1804375 * b,
            LUMA[0] * r + LUMA[1] * g + LUMA[2] * b,
            0.0193339 * r + 0.1191920 * g + 0.9503041 * b
        };
    }

    /**
     * Converts CIE XYZ D65 to linear [0,1] RGB using the inverse BT.709 matrix.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return linear RGB in [0, 1] (may exceed bounds for out-of-gamut colors)
     */
    static double[] xyzToLinear(double[] xyz) {
        return new double[]{
             3.2404542 * xyz[0] - 1.5371385 * xyz[1] - 0.4985314 * xyz[2],
            -0.9692660 * xyz[0] + 1.8760108 * xyz[1] + 0.0415560 * xyz[2],
             0.0556434 * xyz[0] - 0.2040259 * xyz[1] + 1.0572252 * xyz[2]
        };
    }

    // ===== COLORSPACE OVERRIDES =====
    
    @Override
    public double formatRaw(double value) {
        return Math.round(value);
    }

    @Override
    public double[] normalize(double[] raw) {
        return new double[]{raw[0] / 255.0, raw[1] / 255.0, raw[2] / 255.0};
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[]{
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