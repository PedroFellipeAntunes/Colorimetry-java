package colorimetry.spaces.rgb;

import colorimetry.ColorSpace;

/**
 * CMYK (Cyan, Magenta, Yellow, Key/Black) color space descriptor.
 *
 * Extracts the common black component from CMY to save ink.
 * K = min(C, M, Y), then C/M/Y are adjusted relative to K.
 */
public final class Cmyk implements ColorSpace {
    public static final Cmyk INSTANCE = new Cmyk();

    private static final String[] NAMES = {"Cyan", "Magenta", "Yellow", "Key"};
    private static final double[] MAXS = {100.0, 100.0, 100.0, 100.0};

    private Cmyk() {}

    @Override
    public String displayName() {
        return "CMYK";
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
        return 0.0;
    }
    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Cmy.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // CMYK [0,100] -> CMY [0,100]
        double c = raw[0] / 100.0;
        double m = raw[1] / 100.0;
        double y = raw[2] / 100.0;
        double k = raw[3] / 100.0;

        return new double[] {
            (c * (1.0 - k) + k) * 100.0,
            (m * (1.0 - k) + k) * 100.0,
            (y * (1.0 - k) + k) * 100.0
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // CMY [0,100] -> CMYK [0,100]
        double c = parentRaw[0] / 100.0;
        double m = parentRaw[1] / 100.0;
        double y = parentRaw[2] / 100.0;

        double k = Math.min(c, Math.min(m, y));

        if (k >= 1.0) {
            return new double[] {0.0, 0.0, 0.0, 100.0};
        }

        double invK = 1.0 / (1.0 - k);

        return new double[] {
            (c - k) * invK * 100.0,
            (m - k) * invK * 100.0,
            (y - k) * invK * 100.0,
            k * 100.0
        };
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / MAXS[0],
            raw[1] / MAXS[1],
            raw[2] / MAXS[2],
            raw[3] / MAXS[3]
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * MAXS[0],
            normalized[1] * MAXS[1],
            normalized[2] * MAXS[2],
            normalized[3] * MAXS[3]
        };
    }
}