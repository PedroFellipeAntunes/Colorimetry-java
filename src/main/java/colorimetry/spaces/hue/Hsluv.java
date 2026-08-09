package colorimetry.spaces.hue;

import colorimetry.ColorSpace;
import colorimetry.spaces.cie.CieLchuv;

/**
 * HSLuv color space descriptor.
 *
 * Source: Alexei Boronine, 2012. https://www.hsluv.org/math/
 *
 * A human-friendly alternative to HSL where saturation is normalized
 * to the maximum chroma achievable in sRGB at the given lightness and hue.
 * S=100 always produces the most saturated sRGB color possible.
 *
 * Uses ray-line intersection against the 6 sRGB gamut boundary lines
 * projected into the CIE LCHuv chromaticity plane.
 */
public final class Hsluv implements ColorSpace {
    public static final Hsluv INSTANCE = new Hsluv();

    private static final String[] NAMES = {"Hue", "Saturation", "Lightness"};
    private static final double[] MAXS = {360.0, 100.0, 100.0};

    // CIE Lab constants
    private static final double KAPPA = 903.2962962;
    private static final double EPSILON = 0.0088564516;

    // sRGB XYZ to linear RGB matrix (same as BT.709 inverse)
    private static final double[][] M_INV = {
        { 3.2404541621, -1.5371385940, -0.4985314096},
        {-0.9692660305,  1.8760108454,  0.0415560175},
        { 0.0556434309, -0.2040259135,  1.0572251882}
    };

    private Hsluv() {}

    @Override
    public String displayName() {
        return "HSLuv";
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
     * Computes the 6 bounding lines of the sRGB gamut in the chroma-hue
     * plane at a given CIE Luv lightness. Each line is (slope, intercept)
     * in polar coordinates.
     *
     * @param L lightness in [0, 100]
     * @return 6 pairs of (slope, intercept)
     */
    static double[][] getBounds(double L) {
        double[][] result = new double[6][2];
        double sub1 = Math.pow(L + 16.0, 3.0) / 1560896.0;
        double sub2 = sub1 > EPSILON ? sub1 : L / KAPPA;

        for (int c = 0; c < 3; c++) {
            double m1 = M_INV[c][0];
            double m2 = M_INV[c][1];
            double m3 = M_INV[c][2];

            for (int t = 0; t < 2; t++) {
                double top1 = (284517.0 * m1 - 94839.0 * m3) * sub2;
                double top2 = (838422.0 * m3 + 769860.0 * m2 + 731718.0 * m1) * L * sub2 - 769860.0 * t * L;
                double bottom = (632260.0 * m3 - 126452.0 * m2) * sub2 + 126452.0 * t;

                result[c * 2 + t][0] = top1 / bottom;
                result[c * 2 + t][1] = top2 / bottom;
            }
        }

        return result;
    }

    /**
     * Maximum chroma achievable in sRGB at the given lightness and hue.
     *
     * @param L lightness in [0, 100]
     * @param H hue in degrees [0, 360)
     * @return maximum chroma
     */
    private static double maxChromaForLH(double L, double H) {
        double hrad = Math.toRadians(H);
        double minLen = Double.MAX_VALUE;

        for (double[] bound : getBounds(L)) {
            double len = bound[1] / (Math.sin(hrad) - bound[0] * Math.cos(hrad));

            if (len >= 0.0) {
                minLen = Math.min(minLen, len);
            }
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

        double maxC = maxChromaForLH(L, H);
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

        double maxC = maxChromaForLH(L, H);
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