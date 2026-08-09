package colorimetry.spaces.perceptual;

import colorimetry.ColorSpace;

/**
 * IPT color space descriptor.
 *
 * Source: Ebner and Fairchild, "Development and Testing of a Color Space
 *         (IPT) with Improved Hue Uniformity", IS&amp;T 6th Color Imaging
 *         Conference, 1998.
 *
 * Applies power 0.43 compression to LMS, then an opponent transform.
 * I is intensity (lightness), P is protan (red-green), T is tritan (yellow-blue).
 */
public final class Ipt implements ColorSpace {
    public static final Ipt INSTANCE = new Ipt();

    private static final String[] NAMES = {"Intensity", "Protan", "Tritan"};
    private static final double[] MINS = {0.0, -1.0, -1.0};
    private static final double[] MAXS = {1.0, 1.0, 1.0};
    private static final double[] DEFAULTS = {0.5, 0.0, 0.0};

    private static final double GAMMA = 0.43;
    private static final double INV_GAMMA = 1.0 / 0.43;

    // LMS' (power-compressed) -> IPT
    private static final double[][] LMS_TO_IPT = {
        { 0.4000,  0.4000,  0.2000},
        { 4.4550, -4.8510,  0.3960},
        { 0.8056,  0.3572, -1.1628}
    };

    // IPT -> LMS'
    private static final double[][] IPT_TO_LMS = {
        { 1.0000000000,  0.0975689305,  0.2052264332},
        { 1.0000000000, -0.1138764855,  0.1332171584},
        { 1.0000000000,  0.0326151099, -0.6768871831}
    };

    private Ipt() {}

    @Override
    public String displayName() {
        return "IPT";
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
        return 0.001;
    }

    @Override
    public boolean isBounded() {
        return false;
    }

    @Override
    public boolean isChannelBounded(int i) {
        return i == 0;
    }

    // ===== MATH =====

    private static double powerCompress(double x) {
        return Math.signum(x) * Math.pow(Math.abs(x), GAMMA);
    }

    private static double powerExpand(double x) {
        return Math.signum(x) * Math.pow(Math.abs(x), INV_GAMMA);
    }

    private static double[] matMul(double[][] m, double[] v) {
        return new double[] {
            m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]
        };
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return LmsHpe.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double[] lmsP = matMul(IPT_TO_LMS, raw);

        return new double[] {
            powerExpand(lmsP[0]),
            powerExpand(lmsP[1]),
            powerExpand(lmsP[2])
        };
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double[] lmsP = new double[] {
            powerCompress(parentRaw[0]),
            powerCompress(parentRaw[1]),
            powerCompress(parentRaw[2])
        };

        return matMul(LMS_TO_IPT, lmsP);
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / MAXS[0],
            (raw[1] - MINS[1]) / (MAXS[1] - MINS[1]),
            (raw[2] - MINS[2]) / (MAXS[2] - MINS[2])
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * MAXS[0],
            normalized[1] * (MAXS[1] - MINS[1]) + MINS[1],
            normalized[2] * (MAXS[2] - MINS[2]) + MINS[2]
        };
    }
}