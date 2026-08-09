package colorimetry.spaces.hue;

import colorimetry.ColorSpace;
import colorimetry.spaces.perceptual.Oklab;

/**
 * OkHSV color space descriptor.
 *
 * Source: Bjorn Ottosson, "Okhsv and Okhsl - Two new color spaces for
 *         color picking", 2021.
 *         https://bottosson.github.io/posts/colorpicker/
 *
 * Perceptual HSV based on Oklab. Produces a cone-like shape similar to
 * traditional HSV but with perceptually uniform hue and lightness.
 */
public final class OkHsv implements ColorSpace {
    public static final OkHsv INSTANCE = new OkHsv();

    private static final String[] NAMES = {"Hue", "Saturation", "Value"};
    private static final double[] MAXS = {360.0, 1.0, 1.0};

    private OkHsv() {}

    @Override
    public String displayName() {
        return "Ok HSV";
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
        return (i == 0) ? 0.0 : 1.0;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Oklab.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double H = raw[0];
        double S = raw[1];
        double V = raw[2];

        if (V <= 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        double a_ = Math.cos(Math.toRadians(H));
        double b_ = Math.sin(Math.toRadians(H));

        double[] st = OkHsl.computeSt(a_, b_);
        double sMax = st[0];
        double tMax = st[1];
        double s0 = 0.5;
        double k = 1.0 - s0 / sMax;

        double lv = 1.0 - S * s0 / (s0 + tMax - tMax * k * S);
        double cv = S * tMax * s0 / (s0 + tMax - tMax * k * S);

        double L = V * lv;
        double C = V * cv;

        return new double[] {L, C * a_, C * b_};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double L = parentRaw[0];
        double a = parentRaw[1];
        double b = parentRaw[2];

        double C = Math.sqrt(a * a + b * b);
        double H = Math.toDegrees(Math.atan2(b, a));

        if (H < 0.0) {
            H += 360.0;
        }

        if (L <= 0.0) {
            return new double[] {H, 0.0, 0.0};
        }

        double a_ = (C == 0.0) ? 1.0 : a / C;
        double b_ = (C == 0.0) ? 0.0 : b / C;

        double[] st = OkHsl.computeSt(a_, b_);
        double sMax = st[0];
        double tMax = st[1];
        double s0 = 0.5;
        double k = 1.0 - s0 / sMax;

        double V = (C == 0.0) ? L : L / (L - C * k * tMax / (C + tMax * s0));
        double S = (C == 0.0) ? 0.0 : (s0 + tMax) * C / (tMax * s0 + tMax * k * C);

        return new double[] {H, S, V};
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