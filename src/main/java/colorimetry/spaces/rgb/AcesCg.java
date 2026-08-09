package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.xyz.XyzD60;

/**
 * ACEScg (AP1) linear RGB color space descriptor.
 *
 * Source: SMPTE S-2014-004, "ACEScg - A Working Space for CGI Render
 *         and Compositing".
 *         Primaries: R(0.713, 0.293), G(0.165, 0.830), B(0.128, 0.044).
 *         White point: D60.
 */
public final class AcesCg implements RgbLike {
    public static final AcesCg INSTANCE = new AcesCg();

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    private static final double[] LUMA = {0.2722287168, 0.6740817658, 0.0536895174};

    private AcesCg() {}

    @Override
    public String displayName() {
        return "ACEScg";
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
        return 1.0;
    }

    @Override
    public double componentDefault(int i) {
        return 0.0;
    }

    @Override
    public double componentStep(int i) {
        return 0.001;
    }

    @Override
    public boolean isBounded() {
        return false;
    }

    @Override
    public boolean isInGamut(double[] xyz) {
        return true;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return XyzD60.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        return new double[] {
            0.6624541811 * raw[0] + 0.1340042065 * raw[1] + 0.1561876870 * raw[2],
            LUMA[0] * raw[0] + LUMA[1] * raw[1] + LUMA[2] * raw[2],
           -0.0055746495 * raw[0] + 0.0040607335 * raw[1] + 1.0103391003 * raw[2]
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        return new double[] {
             1.6410233797 * parentRaw[0] - 0.3248032942 * parentRaw[1] - 0.2364246952 * parentRaw[2],
            -0.6636628587 * parentRaw[0] + 1.6153315917 * parentRaw[1] + 0.0167563477 * parentRaw[2],
             0.0117218943 * parentRaw[0] - 0.0082844420 * parentRaw[1] + 0.9883948585 * parentRaw[2]
        };
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return raw.clone();
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return normalized.clone();
    }

    @Override
    public double[] lumaCoefficients() {
        return LUMA.clone();
    }

    @Override
    public double[] neutralXyz() {
        return toParent(new double[] {0.5, 0.5, 0.5});
    }
}