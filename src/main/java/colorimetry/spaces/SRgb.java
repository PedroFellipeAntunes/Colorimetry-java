package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * sRGB color space descriptor.
 *
 * Source: IEC 61966-2-1:1999, "Multimedia systems and equipment -
 *         Colour measurement and management".
 */
public final class SRgb implements ColorSpace {
    public static final SRgb INSTANCE = new SRgb();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    private SRgb() {}

    @Override public String displayName() {
        return "sRGB";
    }
    
    @Override public int componentCount() {
        return NAMES.length;
    }
    
    @Override public double componentMin(int i) {
        return 0.0;
    }
    
    @Override public double componentMax(int i) {
        return 255.0;
    }
    
    @Override public double componentDefault(int i) {
        return 0.0;
    }
    
    @Override public boolean isBounded() {
        return true;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
    }

    // ===== MATH =====

    /**
     * sRGB gamma decode (IEC 61966-2-1). Piecewise: linear below 0.04045,
     * power curve above. Converts a [0,1] gamma-encoded value to linear light.
     *
     * @param v gamma-encoded value in [0, 1]
     * @return linear-light value
     */
    private static double toLinear(double v) {
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /**
     * sRGB gamma encode (IEC 61966-2-1). Piecewise: linear below 0.0031308,
     * power curve above. Mirrors negative inputs so out-of-gamut values
     * round-trip without NaN from fractional exponents.
     *
     * @param v linear-light value
     * @return gamma-encoded value
     */
    private static double toGamma(double v) {
        if (v < 0.0) {
            return -toGamma(-v);
        }
        
        return v <= 0.0031308 ? 12.92 * v : 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Rgb.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // sRGB gamma [0,255] → linear [0,1] → Rgb raw [0,255]
        return new double[] {
            toLinear(raw[0] / 255.0) * 255.0,
            toLinear(raw[1] / 255.0) * 255.0,
            toLinear(raw[2] / 255.0) * 255.0
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Rgb raw [0,255] → linear [0,1] → sRGB gamma [0,255]
        return new double[] {
            toGamma(parentRaw[0] / 255.0) * 255.0,
            toGamma(parentRaw[1] / 255.0) * 255.0,
            toGamma(parentRaw[2] / 255.0) * 255.0
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
}