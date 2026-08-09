package colorimetry.spaces.cam;

import colorimetry.ColorSpace;

/**
 * CAM16-UCS (Uniform Color Space) descriptor.
 *
 * Source: Li, Li, Wang, Zu, Luo, Cui, 2017.
 *         Applies a compressive transform to CAM16 J and M (colorfulness)
 *         to improve perceptual uniformity for color difference evaluation.
 *
 * Stores J' (compressed lightness), a' and b' (Cartesian from compressed
 * colorfulness M' and hue h).
 */
public final class Cam16Ucs implements ColorSpace {
    public static final Cam16Ucs INSTANCE = new Cam16Ucs();

    // ===== METADATA =====

    private static final String[] NAMES = {"Lightness", "Green-Red", "Blue-Yellow"};
    private static final double[] MINS = {0.0, -100.0, -100.0};
    private static final double[] MAXS = {100.0, 100.0, 100.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    // UCS compression constants
    private static final double C1 = 0.007;
    private static final double C2 = 0.0228;

    private Cam16Ucs() {}

    @Override
    public String displayName() {
        return "CAM16 UCS";
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
        return MINS[i];
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
        return 0.1;
    }
    
    @Override
    public boolean isChannelBounded(int i) {
        // J' [0, 100] is bounded; a' and b' are not
        return i == 0;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Cam16.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double jPrime = raw[0];
        double aPrime = raw[1];
        double bPrime = raw[2];

        // J' → J: invert compressive lightness
        double J = jPrime / (1.0 + 100.0 * C1 - C1 * jPrime);

        // Recover M' and h from Cartesian a', b'
        double mPrime = Math.sqrt(aPrime * aPrime + bPrime * bPrime);
        double hDeg = Math.toDegrees(Math.atan2(bPrime, aPrime));

        if (hDeg < 0.0) {
            hDeg += 360.0;
        }

        // M' → M: invert compressive colorfulness
        double M = mPrime / (1.0 + 100.0 * C2 - C2 * mPrime);

        // M → C: colorfulness to chroma via FL
        double C = M / Math.pow(Cam16.fl(), 0.25);

        return new double[] {J, C, hDeg};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double J = parentRaw[0];
        double chroma = parentRaw[1];
        double hDeg = parentRaw[2];

        // J → J': compressive lightness
        double jPrime = (1.0 + 100.0 * C1) * J / (1.0 + C1 * J);

        // C → M: chroma to colorfulness via FL
        double M = chroma * Math.pow(Cam16.fl(), 0.25);

        // M → M': compressive colorfulness
        double mPrime = (1.0 + 100.0 * C2) * M / (1.0 + C2 * M);

        // M', h → Cartesian a', b'
        double hRad = Math.toRadians(ColorSpace.wrap(hDeg, 360.0));

        return new double[] {jPrime, mPrime * Math.cos(hRad), mPrime * Math.sin(hRad)};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[]{
            raw[0] / 100.0,
            (raw[1] - MINS[1]) / (MAXS[1] - MINS[1]),
            (raw[2] - MINS[2]) / (MAXS[2] - MINS[2])
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[]{
            normalized[0] * 100.0,
            normalized[1] * (MAXS[1] - MINS[1]) + MINS[1],
            normalized[2] * (MAXS[2] - MINS[2]) + MINS[2]
        };
    }
}