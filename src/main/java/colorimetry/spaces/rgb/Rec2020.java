package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;

/**
 * Rec. 2020 (BT.2020) color space descriptor.
 *
 * Source: ITU-R BT.2020-2 (2015), ultra-wide gamut for UHDTV.
 *         Transfer function uses 10-bit precision constants
 *         (alpha = 1.099, beta = 0.018).
 *
 * Parent is Bt2020RgbLinear, which handles the primary matrix. This space
 * only applies/removes the BT.2020 transfer curve.
 */
public final class Rec2020 implements ColorSpace {
    public static final Rec2020 INSTANCE = new Rec2020();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};

    // BT.2020 transfer function constants (10-bit precision)
    private static final double ALPHA = 1.099;
    private static final double BETA = 0.018;

    private Rec2020() {}

    @Override
    public String displayName() {
        return "Rec. 2020";
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
     * BT.2020 OETF inverse (gamma decode). Piecewise: linear below
     * 4.5 * beta, power curve above.
     *
     * @param v gamma-encoded value in [0, 1]
     * @return linear-light value
     */
    private static double toLinear(double v) {
        if (v < 0.0) {
            return -toLinear(-v);
        }

        return v < BETA * 4.5
            ? v / 4.5
            : Math.pow((v + (ALPHA - 1.0)) / ALPHA, 1.0 / 0.45);
    }

    /**
     * BT.2020 OETF (gamma encode). Piecewise: linear below beta,
     * power curve above.
     *
     * @param v linear-light value
     * @return gamma-encoded value
     */
    private static double toGamma(double v) {
        if (v < 0.0) {
            return -toGamma(-v);
        }

        return v < BETA
            ? 4.5 * v
            : ALPHA * Math.pow(v, 0.45) - (ALPHA - 1.0);
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Bt2020RgbLinear.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // Rec.2020 gamma [0,255] → linear [0,1] → parent raw [0,255]
        return new double[] {
            toLinear(raw[0] / 255.0) * 255.0,
            toLinear(raw[1] / 255.0) * 255.0,
            toLinear(raw[2] / 255.0) * 255.0
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Parent raw [0,255] → linear [0,1] → Rec.2020 gamma [0,255]
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