package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;
import colorimetry.types.RgbLike;
import colorimetry.spaces.xyz.XyzD60;

/**
 * ACES 2065-1 (AP0) linear RGB color space descriptor.
 *
 * Source: SMPTE ST 2065-1:2012, "Academy Color Encoding Specification".
 *         Primaries: R(0.7347, 0.2653), G(0.0, 1.0), B(0.0001, -0.077).
 *         White point: D60.
 */
public final class Aces2065 implements RgbLike {
    public static final Aces2065 INSTANCE = new Aces2065();

    private static final String[] NAMES = {"Red", "Green", "Blue"};
    private static final double[] LUMA = {0.3439664498, 0.7281660966, -0.0721325464};

    private Aces2065() {}

    @Override
    public String displayName() {
        return "ACES 2065-1";
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
            0.9525523959 * raw[0] + 0.0000000000 * raw[1] + 0.0000936786 * raw[2],
            LUMA[0] * raw[0] + LUMA[1] * raw[1] + LUMA[2] * raw[2],
            0.0000000000 * raw[0] + 0.0000000000 * raw[1] + 1.0088251844 * raw[2]
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        return new double[] {
             1.0498110175 * parentRaw[0] + 0.0000000000 * parentRaw[1] - 0.0000974845 * parentRaw[2],
            -0.4959030231 * parentRaw[0] + 1.3733130458 * parentRaw[1] + 0.0982400361 * parentRaw[2],
             0.0000000000 * parentRaw[0] + 0.0000000000 * parentRaw[1] + 0.9912520182 * parentRaw[2]
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