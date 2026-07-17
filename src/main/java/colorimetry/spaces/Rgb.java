package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * Linear RGB color space descriptor.
 *
 * Source: IEC 61966-2-1:1999, same primaries and white point as sRGB
 *         but without the gamma transfer function.
 */
public final class Rgb implements ColorSpace {
    public static final Rgb INSTANCE = new Rgb();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    // D65 white point at 50% linear intensity, used as gamut mapping anchor
    private static final double[] NEUTRAL_XYZ = {0.47524, 0.50000, 0.54442};

    private Rgb() {}

    @Override
    public String displayName() {
        return "RGB";
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
        return Xyz.INSTANCE;
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
     * Converts linear [0,1] RGB to CIE XYZ D65 using the sRGB/BT.709 primaries matrix.
     *
     * @param r red channel in [0, 1]
     * @param g green channel in [0, 1]
     * @param b blue channel in [0, 1]
     * @return CIE XYZ D65 triplet
     */
    private static double[] linearToXyz(double r, double g, double b) {
        return new double[]{
            0.4124564 * r + 0.3575761 * g + 0.1804375 * b,
            0.2126729 * r + 0.7151522 * g + 0.0721750 * b,
            0.0193339 * r + 0.1191920 * g + 0.9503041 * b
        };
    }

    /**
     * Converts CIE XYZ D65 to linear [0,1] RGB using the inverse sRGB/BT.709 matrix.
     *
     * @param xyz CIE XYZ D65 triplet
     * @return linear RGB in [0, 1] (may exceed bounds for out-of-gamut colors)
     */
    private static double[] xyzToLinear(double[] xyz) {
        return new double[]{
             3.2404542 * xyz[0] - 1.5371385 * xyz[1] - 0.4985314 * xyz[2],
            -0.9692660 * xyz[0] + 1.8760108 * xyz[1] + 0.0415560 * xyz[2],
             0.0556434 * xyz[0] - 0.2040259 * xyz[1] + 1.0572252 * xyz[2]
        };
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[]{raw[0] / 255.0, raw[1] / 255.0, raw[2] / 255.0};
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[]{
            Math.round(normalized[0] * 255.0),
            Math.round(normalized[1] * 255.0),
            Math.round(normalized[2] * 255.0)
        };
    }

    @Override
    public boolean isInGamut(double[] xyz) {
        double[] rgb = xyzToLinear(xyz);
        // Epsilon tolerance absorbs floating-point drift from XYZ round-trips
        double eps = 1e-6;
        
        return rgb[0] >= -eps && rgb[0] <= 1.0 + eps
            && rgb[1] >= -eps && rgb[1] <= 1.0 + eps
            && rgb[2] >= -eps && rgb[2] <= 1.0 + eps;
    }

    @Override
    public double[] neutralXyz() {
        return NEUTRAL_XYZ.clone();
    }
}