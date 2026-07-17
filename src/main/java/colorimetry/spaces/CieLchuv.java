package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * CIE LCH(uv) color space descriptor. Polar (cylindrical) form of CIE L*u*v*.
 *
 * Source: CIE 15:2004. L* is lightness, C is chroma (distance from neutral axis),
 *         H is hue angle in degrees.
 */
public final class CieLchuv implements ColorSpace {
    public static final CieLchuv INSTANCE = new CieLchuv();

    private static final String[] NAMES = {"Lightness", "Chroma", "Hue"};
    private static final double[] MINS = {0.0, 0.0, 0.0};

    // Practical chroma bound for sRGB gamut in LUV space
    private static final double C_MAX = 180.0;

    private static final double[] MAXS = {100.0, C_MAX, 360.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    private CieLchuv() {}

    @Override
    public String displayName() {
        return "CIE LCHuv";
    }

    @Override
    public int componentCount() {
        return 3;
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
        return 1.0;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return CieLuv.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // LCh → Luv: polar to cartesian
        double hRad = Math.toRadians(((raw[2] % 360.0) + 360.0) % 360.0);
        
        return new double[] {
            raw[0],
            raw[1] * Math.cos(hRad),
            raw[1] * Math.sin(hRad)
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // Luv → LCh: cartesian to polar
        double C = Math.sqrt(parentRaw[1] * parentRaw[1] + parentRaw[2] * parentRaw[2]);
        double H = Math.toDegrees(Math.atan2(parentRaw[2], parentRaw[1]));

        if (H < 0.0) {
            H += 360.0;
        }

        return new double[] {parentRaw[0], C, H};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / 100.0,
            raw[1] / C_MAX,
            ((raw[2] % 360.0) + 360.0) % 360.0 / 360.0
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * 100.0,
            normalized[1] * C_MAX,
            normalized[2] * 360.0
        };
    }
}