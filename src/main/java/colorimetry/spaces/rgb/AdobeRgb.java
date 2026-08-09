package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;

/**
 * Adobe RGB (1998) color space descriptor.
 *
 * Source: Adobe Systems, "Adobe RGB (1998) Color Image Encoding", 2005.
 *         Wider green gamut than sRGB, standard in photography workflows.
 *
 * Parent is AdobeRgbLinear, which handles the primary matrix. This space
 * only applies/removes the pure power gamma (563/256 ≈ 2.19921875).
 */
public final class AdobeRgb implements ColorSpace {
    public static final AdobeRgb INSTANCE = new AdobeRgb();

    // ===== METADATA =====

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    // Pure power gamma: 563/256 ≈ 2.19921875
    private static final double GAMMA = 563.0 / 256.0;
    private static final double INV_GAMMA = 256.0 / 563.0;

    private AdobeRgb() {}

    @Override
    public String displayName() {
        return "Adobe RGB";
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
     * Adobe RGB gamma decode. Pure power function, no linear segment.
     * Mirrors negative inputs for out-of-gamut safety.
     *
     * @param v gamma-encoded value in [0, 1]
     * @return linear-light value
     */
    private static double toLinear(double v) {
        if (v < 0.0) {
            return -Math.pow(-v, GAMMA);
        }

        return Math.pow(v, GAMMA);
    }

    /**
     * Adobe RGB gamma encode. Pure power function.
     * Mirrors negative inputs for out-of-gamut safety.
     *
     * @param v linear-light value
     * @return gamma-encoded value
     */
    private static double toGamma(double v) {
        if (v < 0.0) {
            return -Math.pow(-v, INV_GAMMA);
        }

        return Math.pow(v, INV_GAMMA);
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return AdobeRgbLinear.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // Adobe gamma [0,255] → linear [0,1] → parent raw [0,255]
        return new double[] {
            toLinear(raw[0] / 255.0) * 255.0,
            toLinear(raw[1] / 255.0) * 255.0,
            toLinear(raw[2] / 255.0) * 255.0
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Parent raw [0,255] → linear [0,1] → Adobe gamma [0,255]
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