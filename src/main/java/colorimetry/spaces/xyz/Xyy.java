package colorimetry.spaces.xyz;

import colorimetry.ColorSpace;

/**
 * CIE xyY chromaticity color space descriptor.
 *
 * Source: CIE 1931 color space. Separates chromaticity (x, y) from luminance (Y).
 *         Used for chromaticity diagrams and gamut boundary visualization.
 */
public final class Xyy implements ColorSpace {
    public static final Xyy INSTANCE = new Xyy();

    // ===== METADATA =====

    private static final String[] NAMES = {"x", "y", "Y"};
    private static final double[] MAXS = {1.0, 1.0, 1.0};
    // D65 chromaticity as default x, y; mid luminance
    private static final double[] DEFAULTS = {0.3127, 0.3290, 0.5};

    private Xyy() {}

    @Override
    public String displayName() {
        return "xyY";
    }

    @Override
    public int componentCount() {
        return NAMES.length;
    }

    @Override
    public String componentName(int i, boolean full) {
        return NAMES[i];
    }

    @Override
    public double componentMin(int i) {
        return 0.0;
    }

    @Override
    public double componentMax(int i) {
        return MAXS[i];
    }

    @Override
    public double componentDefault(int i) {
        return DEFAULTS[i];
    }

    @Override
    public double componentStep(int i) {
        return 0.001;
    }
    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return XyzD65.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double x = raw[0];
        double y = raw[1];
        double bigY = raw[2];

        // y = 0 means black; X and Z would be undefined
        if (y == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        // Recover X and Z from chromaticity and luminance
        double bigX = x * bigY / y;
        double bigZ = (1.0 - x - y) * bigY / y;

        return new double[] {bigX, bigY, bigZ};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double bigX = parentRaw[0];
        double bigY = parentRaw[1];
        double bigZ = parentRaw[2];
        double sum = bigX + bigY + bigZ;

        // Black: all tristimulus zero, chromaticity undefined
        if (sum == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        // Project onto the chromaticity plane
        return new double[] {bigX / sum, bigY / sum, bigY};
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

}