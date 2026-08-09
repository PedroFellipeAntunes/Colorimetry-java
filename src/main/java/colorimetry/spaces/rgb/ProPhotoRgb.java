package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;

/**
 * ProPhoto RGB (ROMM RGB) color space descriptor.
 *
 * Source: Kodak, "ROMM RGB Color Encoding Specification", 1999.
 *         Very wide gamut covering ~90% of visible colors.
 *
 * Parent is ProPhotoRgbLinear, which handles the primary matrix and D50
 * illuminant. This space only applies/removes the gamma 1.8 curve
 * with linear segment.
 */
public final class ProPhotoRgb implements ColorSpace {
    public static final ProPhotoRgb INSTANCE = new ProPhotoRgb();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    // Gamma 1.8 with linear segment threshold
    private static final double GAMMA = 1.8;
    private static final double INV_GAMMA = 1.0 / 1.8;
    private static final double LINEAR_THRESHOLD = 1.0 / 512.0;
    private static final double ENCODED_THRESHOLD = 16.0 / 512.0;

    private ProPhotoRgb() {}

    @Override
    public String displayName() {
        return "ProPhoto RGB";
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
     * ProPhoto gamma decode. Piecewise: linear below 16/512,
     * power 1.8 above.
     *
     * @param v gamma-encoded value in [0, 1]
     * @return linear-light value
     */
    private static double toLinear(double v) {
        if (v < 0.0) {
            return -toLinear(-v);
        }

        return v < ENCODED_THRESHOLD ? v / 16.0 : Math.pow(v, GAMMA);
    }

    /**
     * ProPhoto gamma encode. Piecewise: linear below 1/512,
     * power 1/1.8 above.
     *
     * @param v linear-light value
     * @return gamma-encoded value
     */
    private static double toGamma(double v) {
        if (v < 0.0) {
            return -toGamma(-v);
        }

        return v < LINEAR_THRESHOLD ? 16.0 * v : Math.pow(v, INV_GAMMA);
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return ProPhotoRgbLinear.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // ProPhoto gamma [0,255] → linear [0,1] → parent raw [0,255]
        return new double[] {
            toLinear(raw[0] / 255.0) * 255.0,
            toLinear(raw[1] / 255.0) * 255.0,
            toLinear(raw[2] / 255.0) * 255.0
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Parent raw [0,255] → linear [0,1] → ProPhoto gamma [0,255]
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