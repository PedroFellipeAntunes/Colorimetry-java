package colorimetry.spaces.hue;

import colorimetry.ColorSpace;
import colorimetry.spaces.cie.CieLchuv;

/**
 * HPLuv color space descriptor.
 *
 * Source: Alexei Boronine, 2012. https://www.hsluv.org/math/
 *
 * Pastel variant of HSLuv. Uses the minimum perpendicular distance to the
 * sRGB gamut boundary lines instead of ray intersection. This guarantees
 * that all hues at a given S and L produce valid sRGB colors, but limits
 * the output to pastel tones only.
 */
public final class Hpluv implements ColorSpace {
    public static final Hpluv INSTANCE = new Hpluv();

    private static final String[] NAMES = {"Hue", "Saturation", "Lightness"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};

    private Hpluv() {}

    @Override
    public String displayName() {
        return "HPLuv";
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
        return MAXS[i];
    }

    @Override
    public double componentDefault(int i) {
        if (i == 1) {
            return 100.0;
        }

        if (i == 2) {
            return 50.0;
        }

        return 0.0;
    }

    // ===== MATH =====

    /**
     * Maximum chroma that fits all hues in sRGB at the given lightness.
     * Uses perpendicular distance to the boundary lines instead of
     * ray intersection, producing the inscribed circle.
     *
     * @param L lightness in [0, 100]
     * @return maximum safe chroma for all hues
     */
    private static double maxSafeChromaForL(double L) {
        double minLen = Double.MAX_VALUE;

        for (double[] bound : Hsluv.getBounds(L)) {
            double len = Math.abs(bound[1]) / Math.sqrt(bound[0] * bound[0] + 1.0);
            minLen = Math.min(minLen, len);
        }

        return minLen;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return CieLchuv.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double H = raw[0];
        double S = raw[1];
        double L = raw[2];

        if (L > 99.9999999) {
            return new double[] {100.0, 0.0, H};
        }

        if (L < 0.00000001) {
            return new double[] {0.0, 0.0, H};
        }

        double maxC = maxSafeChromaForL(L);
        double C = maxC / 100.0 * S;

        return new double[] {L, C, H};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double L = parentRaw[0];
        double C = parentRaw[1];
        double H = parentRaw[2];

        if (L > 99.9999999) {
            return new double[] {H, 0.0, 100.0};
        }

        if (L < 0.00000001) {
            return new double[] {H, 0.0, 0.0};
        }

        double maxC = maxSafeChromaForL(L);
        double S = C / maxC * 100.0;

        return new double[] {H, S, L};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            ColorSpace.wrap(raw[0], MAXS[0]) / MAXS[0],
            ColorSpace.clamp(raw[1] / MAXS[1], 0.0, 1.0),
            ColorSpace.clamp(raw[2] / MAXS[2], 0.0, 1.0)
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * MAXS[0],
            normalized[1] * MAXS[1],
            normalized[2] * MAXS[2]
        };
    }

    @Override
    public boolean hasPalette() {
        return true;
    }

    @Override
    public int[] paletteChannels() {
        return new int[] {0, 2};
    }

    @Override
    public boolean isCylindrical() {
        return true;
    }

    @Override
    public int hueChannel() {
        return 0;
    }

    @Override
    public int radialChannel() {
        return 1;
    }
}