package colorimetry.spaces.perceptual;

import colorimetry.ColorSpace;
import colorimetry.spaces.xyz.XyzD65;

/**
 * XYB color space descriptor.
 *
 * Source: JPEG XL Image Coding System (ISO/IEC 18181).
 *         White paper: "JPEG XL White Paper", v2.0, 2023.
 *
 * An LMS-based color model for perceptually uniform quantization.
 * Uses a cube root (gamma 3) for computationally efficient decoding.
 * Y functions as lightness, X and B as opponent channels.
 */
public final class Xyb implements ColorSpace {
    public static final Xyb INSTANCE = new Xyb();

    private static final String[] NAMES = {"X", "Y", "B"};
    private static final double[] MINS = {-0.04, 0.0, -0.4};
    private static final double[] MAXS = {0.04, 0.85, 0.4};
    private static final double[] DEFAULTS = {0.0, 0.4, 0.0};

    private static final double BIAS = 0.00379307;
    private static final double BIAS_CBRT = Math.cbrt(BIAS);

    // XYZ D65 -> LMS
    private static final double[][] XYZ_TO_LMS = {
        { 0.3739,  0.6896, -0.0413},
        { 0.0792,  0.9286, -0.0035},
        { 0.6212, -0.1027,  0.4704}
    };

    // LMS -> XYZ D65
    private static final double[][] LMS_TO_XYZ = {
        { 2.7251296427, -1.9989283496,  0.2243869154},
        {-0.2461921239,  1.2583628917, -0.0122522632},
        {-3.6524967372,  2.9144731287,  1.8268548909}
    };

    // LMS' -> XYB
    private static final double[][] LMS_TO_XYB = {
        { 0.5, -0.5, 0.0},
        { 0.5,  0.5, 0.0},
        { 0.0,  0.0, 1.0}
    };

    // XYB -> LMS'
    private static final double[][] XYB_TO_LMS = {
        { 1.0,  1.0, 0.0},
        {-1.0,  1.0, 0.0},
        { 0.0,  0.0, 1.0}
    };

    private Xyb() {}

    @Override
    public String displayName() {
        return "XYB";
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
    // ===== MATH =====

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
        return XyzD65.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        // XYB -> LMS' (biased cube root domain)
        double[] lmsP = matMul(XYB_TO_LMS, raw);

        // Undo cube root and bias
        double[] lms = new double[] {
            Math.pow(lmsP[0] + BIAS_CBRT, 3.0) - BIAS,
            Math.pow(lmsP[1] + BIAS_CBRT, 3.0) - BIAS,
            Math.pow(lmsP[2] + BIAS_CBRT, 3.0) - BIAS
        };

        return matMul(LMS_TO_XYZ, lms);
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        // XYZ -> LMS
        double[] lms = matMul(XYZ_TO_LMS, parentRaw);

        // Biased cube root
        double[] lmsP = new double[] {
            Math.cbrt(lms[0] + BIAS) - BIAS_CBRT,
            Math.cbrt(lms[1] + BIAS) - BIAS_CBRT,
            Math.cbrt(lms[2] + BIAS) - BIAS_CBRT
        };

        // Subtract Y from B for achromatic alignment
        double[] xyb = matMul(LMS_TO_XYB, lmsP);
        xyb[2] -= xyb[1];

        return xyb;
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            (raw[0] - MINS[0]) / (MAXS[0] - MINS[0]),
            raw[1] / MAXS[1],
            (raw[2] - MINS[2]) / (MAXS[2] - MINS[2])
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * (MAXS[0] - MINS[0]) + MINS[0],
            normalized[1] * MAXS[1],
            normalized[2] * (MAXS[2] - MINS[2]) + MINS[2]
        };
    }

}