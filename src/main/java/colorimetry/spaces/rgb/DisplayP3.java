package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;

/**
 * Display P3 color space descriptor.
 *
 * Source: DCI-P3 primaries with D65 white point and sRGB transfer function.
 *         Used by Apple devices, CSS color(), and modern wide-gamut displays.
 *
 * Parent is P3RgbLinear, which handles the primary matrix. This space
 * only applies/removes the sRGB gamma curve.
 */
public final class DisplayP3 implements ColorSpace {
    public static final DisplayP3 INSTANCE = new DisplayP3();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};

    private DisplayP3() {}

    @Override
    public String displayName() {
        return "Display P3";
    }

    @Override
    public int componentCount() {
        return NAMES.length;
    }

    @Override
    public String componentName(int i, boolean full) {
        return full ? NAMES[i] : ColorSpace.shortOf(NAMES[i]);
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
    // ===== MATH =====

    /**
     * sRGB gamma decode. Same transfer function as sRGB (IEC 61966-2-1).
     *
     * @param v gamma-encoded value in [0, 1]
     * @return linear-light value
     */
    private static double toLinear(double v) {
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /**
     * sRGB gamma encode. Mirrors negative inputs for out-of-gamut safety.
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
        return DisplayP3Linear.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // P3 gamma [0,255] → linear [0,1] → parent raw [0,255]
        return new double[] {
            toLinear(raw[0] / 255.0) * 255.0,
            toLinear(raw[1] / 255.0) * 255.0,
            toLinear(raw[2] / 255.0) * 255.0
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Parent raw [0,255] → linear [0,1] → P3 gamma [0,255]
        return new double[] {
            toGamma(parentRaw[0] / 255.0) * 255.0,
            toGamma(parentRaw[1] / 255.0) * 255.0,
            toGamma(parentRaw[2] / 255.0) * 255.0
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
}