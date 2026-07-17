package colorimetry.spaces;

import colorimetry.ColorSpace;

/**
 * CIE L*u*v* (CIELUV) color space descriptor.
 *
 * Source: CIE 15:2004. Designed for emissive (self-luminous) colors such as
 *         displays. Uses D65 illuminant directly — no chromatic adaptation
 *         needed since the library's hub is also D65.
 *
 * Unlike CIE Lab (which uses a nonlinear f(t) on each XYZ channel separately),
 * CIELUV projects XYZ through u'v' chromaticity coordinates. This makes additive
 * color mixing linear in the u'v' plane.
 */
public final class CieLuv implements ColorSpace {
    public static final CieLuv INSTANCE = new CieLuv();

    private static final String[] NAMES = {"L*", "u*", "v*"};
    private static final double[] MINS = {0.0, -200.0, -200.0};
    private static final double[] MAXS = {100.0, 200.0, 200.0};
    private static final double[] DEFAULTS = {50.0, 0.0, 0.0};

    // CIE thresholds for L* piecewise function
    private static final double EPSILON = 216.0 / 24389.0;  // (6/29)³ ≈ 0.008856
    private static final double KAPPA = 24389.0 / 27.0;     // (29/3)³ ≈ 903.3

    // D65 white point u'v' chromaticity (no chromatic adaptation needed)
    private static final double Yn = 1.0;
    private static final double DENOM_N = 0.95047 + 15.0 * 1.0 + 3.0 * 1.08883; // Xn + 15Yn + 3Zn
    private static final double UN = 4.0 * 0.95047 / DENOM_N;  // ≈ 0.19784
    private static final double VN = 9.0 * 1.0 / DENOM_N;      // ≈ 0.46834

    private CieLuv() {}

    @Override
    public String displayName() {
        return "CIE Luv";
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

    // ===== MATH =====

    /**
     * Computes CIE L* from Y/Yn ratio. Piecewise: cube root above epsilon,
     * linear below to avoid infinite slope near zero.
     * Same formula as CIE Lab lightness.
     *
     * @param yRatio Y / Yn
     * @return L* in [0, 100]
     */
    private static double lStar(double yRatio) {
        if (yRatio > EPSILON) {
            return 116.0 * Math.cbrt(yRatio) - 16.0;
        }
        
        return KAPPA * yRatio;
    }

    /**
     * Recovers Y/Yn ratio from L*. Inverse of lStar().
     * Threshold 8.0 corresponds to kappa * epsilon.
     *
     * @param L L* value
     * @return Y / Yn ratio
     */
    private static double yRatio(double L) {
        if (L > 8.0) {
            double t = (L + 16.0) / 116.0;
            
            return t * t * t;
        }
        
        return L / KAPPA;
    }

    // ===== PARENT HIERARCHY =====

    @Override
    public ColorSpace parentSpace() {
        return Xyz.INSTANCE;
    }

    @Override
    public double[] toParent(double[] raw) {
        double L = raw[0];
        double u = raw[1];
        double v = raw[2];

        // L* = 0 means black; u', v' would be undefined (division by zero)
        if (L == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        // Recover u'v' chromaticity from L*, u*, v*
        double uPrime = u / (13.0 * L) + UN;
        double vPrime = v / (13.0 * L) + VN;

        // Recover Y from L*
        double Y = Yn * yRatio(L);

        // Recover X, Z from Y and u'v' chromaticity
        double X = Y * 9.0 * uPrime / (4.0 * vPrime);
        double Z = Y * (12.0 - 3.0 * uPrime - 20.0 * vPrime) / (4.0 * vPrime);

        return new double[] {X, Y, Z};
    }

    @Override
    public double[] fromParent(double[] parentRaw) {
        double X = parentRaw[0];
        double Y = parentRaw[1];
        double Z = parentRaw[2];

        // Denominator for u'v' projection
        double denom = X + 15.0 * Y + 3.0 * Z;

        // Black: X=Y=Z=0, u'v' undefined
        if (denom == 0.0) {
            return new double[] {0.0, 0.0, 0.0};
        }

        // Project XYZ onto u'v' chromaticity plane
        double uPrime = 4.0 * X / denom;
        double vPrime = 9.0 * Y / denom;

        // L* from Y (same formula as CIE Lab)
        double L = lStar(Y / Yn);

        // u*, v* = chromaticity difference from white point, scaled by L*
        double u = 13.0 * L * (uPrime - UN);
        double v = 13.0 * L * (vPrime - VN);

        return new double[] {L, u, v};
    }

    // ===== COLORSPACE OVERRIDES =====

    @Override
    public double[] normalize(double[] raw) {
        return new double[] {
            raw[0] / 100.0,
            (raw[1] + 200.0) / 400.0, // shift [-200,200] to [0,1]
            (raw[2] + 200.0) / 400.0
        };
    }

    @Override
    public double[] denormalize(double[] normalized) {
        return new double[] {
            normalized[0] * 100.0,
            normalized[1] * 400.0 - 200.0, // shift [0,1] back to [-200,200]
            normalized[2] * 400.0 - 200.0
        };
    }

    @Override
    public boolean isBounded() {
        return false;
    }

    @Override
    public boolean isInGamut(double[] xyz) {
        return true;
    }

    @Override
    public double[] neutralXyz() {
        // L*=50, u*=0, v*=0 converted to XYZ
        return toParent(new double[] {50.0, 0.0, 0.0});
    }
}